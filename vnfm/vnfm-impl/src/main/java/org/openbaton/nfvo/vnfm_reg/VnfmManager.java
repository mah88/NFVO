/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.nfvo.vnfm_reg;

import com.google.gson.Gson;
import org.openbaton.catalogue.mano.common.VNFDeploymentFlavour;
import org.openbaton.catalogue.mano.descriptor.*;
import org.openbaton.catalogue.mano.record.*;
import org.openbaton.catalogue.nfvo.*;
import org.openbaton.catalogue.nfvo.messages.Interfaces.NFVMessage;
import org.openbaton.catalogue.nfvo.messages.*;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.VimException;
import org.openbaton.nfvo.common.internal.model.EventFinishNFVO;
import org.openbaton.nfvo.common.internal.model.EventNFVO;
import org.openbaton.nfvo.repositories.NetworkServiceDescriptorRepository;
import org.openbaton.nfvo.repositories.NetworkServiceRecordRepository;
import org.openbaton.nfvo.repositories.VimRepository;
import org.openbaton.nfvo.repositories.VnfPackageRepository;
import org.openbaton.nfvo.vnfm_reg.tasks.*;
import org.openbaton.nfvo.vnfm_reg.tasks.abstracts.AbstractTask;
import org.openbaton.vnfm.interfaces.sender.VnfmSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by lto on 08/07/15.
 */
@Service
@Scope
@Order(value = (Ordered.LOWEST_PRECEDENCE - 10)) // in order to be the second to last
@ConfigurationProperties
public class VnfmManager implements org.openbaton.vnfm.interfaces.manager.VnfmManager, ApplicationEventPublisherAware, ApplicationListener<EventFinishNFVO> {

    private static Map<String, Map<String, Integer>> vnfrNames;
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    @Qualifier("vnfmRegister")
    private VnfmRegister vnfmRegister;
    private ApplicationEventPublisher publisher;
    private ThreadPoolTaskExecutor asyncExecutor;
    @Autowired
    private ConfigurableApplicationContext context;
    @Autowired
    private NetworkServiceRecordRepository nsrRepository;
    @Autowired
    private NetworkServiceDescriptorRepository nsdRepository;
    @Autowired
    private VnfPackageRepository vnfPackageRepository;
    @Autowired
    private VimRepository vimInstanceRepository;
    @Autowired
    private Gson gson;
    @Value("${nfvo.start.ordered:false}")
    private boolean ordered;
    @Value("${nfvo.vmanager.executor.maxpoolsize:30}")
    private int maxPoolSize;
    @Value("${nfvo.vmanager.executor.corepoolsize:5}")
    private int corePoolSize;
    @Value("${nfvo.vmanager.executor.queuecapacity:100}")
    private int queueCapacity;
    @Value("${nfvo.vmanager.executor.keepalive:20}")
    private int keepAliveSeconds;
    @Value("${nfvo.rabbit.brokerIp:127.0.0.1}")
    private String brokerIp;
    @Value("${nfvo.monitoring.ip:}")
    private String monitoringIp;
    @Value("${nfvo.timezone:CET}")
    private String timezone;
    @Value("${nfvo.ems.version:0.15}")
    private String emsVersion;
    @Value("${spring.rabbitmq.username:admin}")
    private String username;
    @Value("${spring.rabbitmq.password:openbaton}")
    private String password;
    @Value("${nfvo.ems.queue.heartbeat:60}")
    private String emsHeartbeat;
    @Value("${nfvo.ems.queue.autodelete:true}")
    private String emsAutodelete;

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public boolean getOrdered() {
        return ordered;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    @Override
    @PostConstruct
    public void init() {

        log.debug("Running VnfmManager init");

        vnfrNames = new LinkedHashMap<>();
        /**
         * Asynchronous thread executor configuration
         */
        this.asyncExecutor = new ThreadPoolTaskExecutor();

        this.asyncExecutor.setThreadNamePrefix("OpenbatonTask-");

        this.asyncExecutor.setMaxPoolSize(maxPoolSize);
        this.asyncExecutor.setCorePoolSize(corePoolSize);
        this.asyncExecutor.setQueueCapacity(queueCapacity);
        this.asyncExecutor.setKeepAliveSeconds(keepAliveSeconds);

        this.asyncExecutor.initialize();

        log.debug("AsyncExecutor is: " + asyncExecutor);

        log.trace("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        log.trace("ThreadPollTaskExecutor configuration:");
        log.trace("MaxPoolSize = " + this.asyncExecutor.getMaxPoolSize());
        log.trace("CorePoolSize = " + this.asyncExecutor.getCorePoolSize());
        log.trace("QueueCapacity = " + this.asyncExecutor.getThreadPoolExecutor().getQueue().size());
        log.trace("KeepAlive = " + this.asyncExecutor.getKeepAliveSeconds());
        log.trace("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

    }

    @Override
    @Async
    public Future<Void> deploy(NetworkServiceDescriptor networkServiceDescriptor, NetworkServiceRecord networkServiceRecord) throws NotFoundException {

        log.debug("Ordered: " + ordered);
        if (ordered) {
            vnfrNames.put(networkServiceRecord.getId(), new HashMap<String, Integer>());
            log.debug("here");
            Map<String, Integer> vnfrNamesWeighted = vnfrNames.get(networkServiceRecord.getId());
            fillVnfrNames(networkServiceDescriptor, vnfrNamesWeighted);
            vnfrNames.put(networkServiceRecord.getId(), sortByValue(vnfrNamesWeighted));

            log.debug("VNFRs ordered by dependencies: " + vnfrNames.get(networkServiceRecord.getId()));
        }

        for (VirtualNetworkFunctionDescriptor vnfd : networkServiceDescriptor.getVnfd()) {
            log.debug("Processing VNFD: " + vnfd.getName());

            Map<String, Collection<VimInstance>> vimInstances = new HashMap<>();

            for (VirtualDeploymentUnit vdu : vnfd.getVdu()) {
                vimInstances.put(vdu.getId(), new ArrayList<VimInstance>());
                for (String vimInstanceName : vdu.getVimInstanceName()) {
                    log.debug("Looking for " + vimInstanceName);
                    vimInstances.get(vdu.getId()).add(vimInstanceRepository.findFirstByName(vimInstanceName));
                }
            }
            log.debug("Found vim instances:");
            for (Map.Entry<String, Collection<VimInstance>> vimInstance : vimInstances.entrySet()){

                if (vimInstance.getValue().size() == 0)
                    for (VimInstance vimInstance1 : vimInstanceRepository.findAll())
                        vimInstance.getValue().add(vimInstance1);
                for (VimInstance vi: vimInstance.getValue())
                    log.debug("\t" + vi.getName());
                log.debug("~~~~~~");
            }

            //Creating the extension
            Map<String, String> extension = getExtension();

            extension.put("nsr-id", networkServiceRecord.getId());

            NFVMessage message;
            if (vnfd.getVnfPackageLocation() != null) {
                VNFPackage vnfPackage = vnfPackageRepository.findFirstById(vnfd.getVnfPackageLocation());
                message = new OrVnfmInstantiateMessage(vnfd, getDeploymentFlavour(vnfd), vnfd.getName(), networkServiceRecord.getVlr(), extension, vimInstances, vnfPackage);
            } else {
                message = new OrVnfmInstantiateMessage(vnfd, getDeploymentFlavour(vnfd), vnfd.getName(), networkServiceRecord.getVlr(), extension, vimInstances, null);
            }
            VnfmManagerEndpoint endpoint = vnfmRegister.getVnfm(vnfd.getEndpoint());
            if (endpoint == null) {
                throw new NotFoundException("VnfManager of type " + vnfd.getType() + " (endpoint = " + vnfd.getEndpoint() + ") is not registered");
            }

            VnfmSender vnfmSender;
            try {
                vnfmSender = this.getVnfmSender(endpoint.getEndpointType());
            } catch (BeansException e) {
                throw new NotFoundException(e);
            }
            vnfmSender.sendCommand(message, endpoint);
            log.info("Sent " + message.getAction() + " to VNF: " + vnfd.getName());
        }
        return new AsyncResult<>(null);
    }

    private Map<String, String> getExtension() {
        Map<String, String> extension = new HashMap<>();
        extension.put("brokerIp", brokerIp.trim());
        extension.put("monitoringIp", monitoringIp.trim());
        extension.put("timezone", timezone);
        extension.put("emsVersion", emsVersion);
        extension.put("username", username);
        extension.put("password", password);
        extension.put("exchangeName", "openbaton-exchange");
        extension.put("emsHeartbeat", emsHeartbeat);
        extension.put("emsAutodelete", emsAutodelete);
        return extension;
    }

    private void fillVnfrNames(NetworkServiceDescriptor networkServiceDescriptor, Map<String, Integer> vnfrNamesWeighted) {

        log.trace("Checking NSD \n\n\n" + networkServiceDescriptor);

        for (VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor : networkServiceDescriptor.getVnfd()) {
            String virtualNetworkFunctionDescriptorName = virtualNetworkFunctionDescriptor.getName();
            int weightForVNFR = getWeightForVNFR(virtualNetworkFunctionDescriptor, networkServiceDescriptor);
            vnfrNamesWeighted.put(virtualNetworkFunctionDescriptorName, weightForVNFR);
            log.debug("Set weight for " + virtualNetworkFunctionDescriptorName + " to " + weightForVNFR);
        }
    }

    private int getWeightForVNFR(VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor, NetworkServiceDescriptor networkServiceDescriptor) {
        int result = 0;
        for (VNFDependency dependency : networkServiceDescriptor.getVnf_dependency()) {
            if (dependency.getTarget().getName().equals(virtualNetworkFunctionDescriptor.getName())) {
                result++;
                result += getWeightForVNFR(dependency.getSource(), networkServiceDescriptor);
            }
        }

        return result;
    }

    //As a default operation of the NFVO, it get always the first DeploymentFlavour!
    private VNFDeploymentFlavour getDeploymentFlavour(VirtualNetworkFunctionDescriptor vnfd) throws NotFoundException {
        if (!vnfd.getDeployment_flavour().iterator().hasNext())
            throw new NotFoundException("There are no DeploymentFlavour in vnfd: " + vnfd.getName());
        return vnfd.getDeployment_flavour().iterator().next();
    }

    @Override
    public VnfmSender getVnfmSender(EndpointType endpointType) throws BeansException {
        String senderName = endpointType.toString().toLowerCase() + "VnfmSender";
        return (VnfmSender) this.context.getBean(senderName);
    }

    @Override
    public String executeAction(NFVMessage nfvMessage) throws VimException, NotFoundException, ExecutionException, InterruptedException {

        String actionName = nfvMessage.getAction().toString().replace("_", "").toLowerCase();
        String beanName = actionName + "Task";
        log.debug("Looking for bean called: " + beanName);
        AbstractTask task = (AbstractTask) context.getBean(beanName);
        log.trace("message: " + nfvMessage);
        task.setAction(nfvMessage.getAction());

        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord;
        if (nfvMessage.getAction().ordinal() == Action.ERROR.ordinal()) {
            VnfmOrErrorMessage vnfmOrErrorMessage = (VnfmOrErrorMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrErrorMessage.getVirtualNetworkFunctionRecord();
            Exception e = vnfmOrErrorMessage.getException();
            ((ErrorTask) task).setException(e);
            ((ErrorTask) task).setNsrId(vnfmOrErrorMessage.getNsrId());
        } else if (nfvMessage.getAction().ordinal() == Action.INSTANTIATE.ordinal()) {
            VnfmOrInstantiateMessage vnfmOrInstantiate = (VnfmOrInstantiateMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrInstantiate.getVirtualNetworkFunctionRecord();
        } else if (nfvMessage.getAction().ordinal() == Action.SCALED.ordinal()) {
            VnfmOrScaledMessage vnfmOrScaled = (VnfmOrScaledMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrScaled.getVirtualNetworkFunctionRecord();
            ((ScaledTask) task).setVnfcInstance(vnfmOrScaled.getVnfcInstance());
        } else if (nfvMessage.getAction().ordinal() == Action.HEAL.ordinal()) {
            VnfmOrHealedMessage vnfmOrHealedMessage = (VnfmOrHealedMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrHealedMessage.getVirtualNetworkFunctionRecord();
            ((HealTask) task).setVnfcInstance(vnfmOrHealedMessage.getVnfcInstance());
            ((HealTask) task).setCause(vnfmOrHealedMessage.getCause());
        } else if (nfvMessage.getAction().ordinal() == Action.SCALING.ordinal()) {
            VnfmOrScalingMessage vnfmOrScalingMessage = (VnfmOrScalingMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrScalingMessage.getVirtualNetworkFunctionRecord();
            ((ScalingTask) task).setUserdata(vnfmOrScalingMessage.getUserData());
        } else if (nfvMessage.getAction().ordinal() == Action.ALLOCATE_RESOURCES.ordinal()) {
            VnfmOrAllocateResourcesMessage vnfmOrAllocateResourcesMessage = (VnfmOrAllocateResourcesMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrAllocateResourcesMessage.getVirtualNetworkFunctionRecord();
            Map<String, VimInstance> vimChosen = vnfmOrAllocateResourcesMessage.getVimInstances();
            ((AllocateresourcesTask) task).setVims(vimChosen);
            ((AllocateresourcesTask) task).setUserData(vnfmOrAllocateResourcesMessage.getUserdata());
        } else {
            VnfmOrGenericMessage vnfmOrGeneric = (VnfmOrGenericMessage) nfvMessage;
            virtualNetworkFunctionRecord = vnfmOrGeneric.getVirtualNetworkFunctionRecord();
            task.setDependency(vnfmOrGeneric.getVnfRecordDependency());
        }

        if (virtualNetworkFunctionRecord != null) {
            if (virtualNetworkFunctionRecord.getParent_ns_id() != null) {
                if (!nsrRepository.exists(virtualNetworkFunctionRecord.getParent_ns_id())) {
                    return null;
                } else {
                    virtualNetworkFunctionRecord.setProjectId(nsrRepository.findFirstById(virtualNetworkFunctionRecord.getParent_ns_id()).getProjectId());
                }
            }

            virtualNetworkFunctionRecord.setTask(actionName);
            task.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);

            log.info("Executing Task " + beanName + " for vnfr " + virtualNetworkFunctionRecord.getName() + ". Cyclic=" + virtualNetworkFunctionRecord.hasCyclicDependency());
        }
        log.trace("AsyncExecutor is: " + asyncExecutor);
        if (isaReturningTask(nfvMessage.getAction()))
            return gson.toJson(asyncExecutor.submit(task).get());
        else {
            asyncExecutor.submit(task);
            return null;
        }
    }

    private boolean isaReturningTask(Action action) {
        switch (action) {
            case ALLOCATE_RESOURCES:
            case GRANT_OPERATION:
            case SCALING:
            case UPDATEVNFR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public synchronized void findAndSetNSRStatus(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {

        if (virtualNetworkFunctionRecord == null) {
            return;
        }

        log.debug("The nsr id is: " + virtualNetworkFunctionRecord.getParent_ns_id());

        Status status = Status.TERMINATED;
        NetworkServiceRecord networkServiceRecord = null;

        // this while loop is necessary because the NSR and it's components might have changed before the save call resulting in an OptimisticLockingFailureException
        boolean foundAndSet = false;
        while (!foundAndSet) {
            try {
                try {
                    networkServiceRecord = nsrRepository.findFirstById(virtualNetworkFunctionRecord.getParent_ns_id());
                } catch (NoResultException e) {
                    log.error("No NSR found with id " + virtualNetworkFunctionRecord.getParent_ns_id());
                    return;
                }

                try {
                    if (nsdRepository.findFirstById(networkServiceRecord.getDescriptor_reference()).getVnfd().size() != networkServiceRecord.getVnfr().size()) {
                        log.debug("Not all the VNFR have been created yet, it is useless to set the NSR status.");
                        return;
                    }
                }catch (NullPointerException e){
                    log.warn("Descriptor was already removed, calculating the status anyway...");
                }

                log.debug("Checking the status of NSR: " + networkServiceRecord.getName());

                for (VirtualNetworkFunctionRecord vnfr : networkServiceRecord.getVnfr()) {
                    log.debug("VNFR " + vnfr.getName() + " is in state: " + vnfr.getStatus());
                    if (status.ordinal() > vnfr.getStatus().ordinal()) {
                        status = vnfr.getStatus();
                    }
                }

                log.debug("Setting NSR status to: " + status);
                networkServiceRecord.setStatus(status);
                networkServiceRecord = nsrRepository.save(networkServiceRecord);
                foundAndSet = true;
            } catch (OptimisticLockingFailureException e) {
                log.info("OptimisticLockingFailureException during findAndSet. Don't worry we will try it again.");
                status = Status.TERMINATED;
            }
        }
        log.debug("Now the status is: " + networkServiceRecord.getStatus());
        if (status.ordinal() == Status.ACTIVE.ordinal()) {
            //Check if all vnfr have been received from the vnfm
            boolean nsrFilledWithAllVnfr = nsdRepository.findFirstById(networkServiceRecord.getDescriptor_reference()).getVnfd().size() == networkServiceRecord.getVnfr().size();
            if (nsrFilledWithAllVnfr)
                publishEvent(Action.INSTANTIATE_FINISH, networkServiceRecord);
            else log.debug("Nsr is ACTIVE but not all vnfr have been received");
        } else if (status.ordinal() == Status.TERMINATED.ordinal()) {
            publishEvent(Action.RELEASE_RESOURCES_FINISH, networkServiceRecord);
            nsrRepository.delete(networkServiceRecord);
        }

        log.debug("Thread: " + Thread.currentThread().getId() + " finished findAndSet");
    }

    private void publishEvent(Action action, Serializable payload) {
        ApplicationEventNFVO event = new ApplicationEventNFVO(action, payload);
        EventNFVO eventNFVO = new EventNFVO(this);
        eventNFVO.setEventNFVO(event);
        log.debug("Publishing event: " + event);
        publisher.publishEvent(eventNFVO);
    }

    @Override
    @Async
    public Future<Void> sendMessageToVNFR(VirtualNetworkFunctionRecord virtualNetworkFunctionRecordDest, NFVMessage nfvMessage) throws NotFoundException {
        VnfmManagerEndpoint endpoint = vnfmRegister.getVnfm(virtualNetworkFunctionRecordDest.getEndpoint());
        if (endpoint == null) {
            throw new NotFoundException("VnfManager of type " + virtualNetworkFunctionRecordDest.getType() + " (endpoint = " + virtualNetworkFunctionRecordDest.getEndpoint() + ") is not registered");
        }
        VnfmSender vnfmSender;
        try {

            vnfmSender = this.getVnfmSender(endpoint.getEndpointType());
        } catch (BeansException e) {
            throw new NotFoundException(e);
        }

        log.debug("Sending message " + nfvMessage.getAction() + " to " + virtualNetworkFunctionRecordDest.getName());
        vnfmSender.sendCommand(nfvMessage, endpoint);
        return new AsyncResult<Void>(null);
    }

    @Override
    @Async
    public Future<Void> release(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) throws NotFoundException {
        VnfmManagerEndpoint endpoint = vnfmRegister.getVnfm(virtualNetworkFunctionRecord.getEndpoint());
        if (endpoint == null) {
            throw new NotFoundException("VnfManager of type " + virtualNetworkFunctionRecord.getType() + " (endpoint = " + virtualNetworkFunctionRecord.getEndpoint() + ") is not registered");
        }

        OrVnfmGenericMessage orVnfmGenericMessage = new OrVnfmGenericMessage(virtualNetworkFunctionRecord, Action.RELEASE_RESOURCES);
        VnfmSender vnfmSender;
        try {

            vnfmSender = this.getVnfmSender(endpoint.getEndpointType());
        } catch (BeansException e) {
            throw new NotFoundException(e);
        }

        vnfmSender.sendCommand(orVnfmGenericMessage, endpoint);
        return new AsyncResult<>(null);
    }

    @Override
    @Async
    public Future<Void> addVnfc(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFComponent component, VNFRecordDependency dependency, String mode) throws NotFoundException {
        VnfmManagerEndpoint endpoint = vnfmRegister.getVnfm(virtualNetworkFunctionRecord.getEndpoint());

        if (endpoint == null) {
            throw new NotFoundException("VnfManager of type " + virtualNetworkFunctionRecord.getType() + " (endpoint = " + virtualNetworkFunctionRecord.getEndpoint() + ") is not registered");
        }

        OrVnfmScalingMessage message = new OrVnfmScalingMessage();
        message.setAction(Action.SCALE_OUT);
        message.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
        message.setVnfPackage(vnfPackageRepository.findFirstById(virtualNetworkFunctionRecord.getPackageId()));
        message.setComponent(component);
        message.setExtension(getExtension());
        message.setDependency(dependency);
        message.setMode(mode);

        log.debug("SCALE_OUT MESSAGE IS: \n" + message);

        VnfmSender vnfmSender;
        try {

            vnfmSender = this.getVnfmSender(endpoint.getEndpointType());
        } catch (BeansException e) {
            throw new NotFoundException(e);
        }

        vnfmSender.sendCommand(message, endpoint);
        return new AsyncResult<>(null);
    }

    @Override
    @Async
    public Future<Void> removeVnfcDependency(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord, VNFCInstance vnfcInstance) throws NotFoundException {
        VnfmManagerEndpoint endpoint = vnfmRegister.getVnfm(virtualNetworkFunctionRecord.getEndpoint());
        if (endpoint == null) {
            throw new NotFoundException("VnfManager of type " + virtualNetworkFunctionRecord.getType() + " (endpoint = " + virtualNetworkFunctionRecord.getEndpoint() + ") is not registered");
        }

        OrVnfmScalingMessage message = new OrVnfmScalingMessage();

        message.setAction(Action.SCALE_IN);
        message.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);
        message.setVnfcInstance(vnfcInstance);
        VnfmSender vnfmSender;
        try {

            vnfmSender = this.getVnfmSender(endpoint.getEndpointType());
        } catch (BeansException e) {
            throw new NotFoundException(e);
        }

        vnfmSender.sendCommand(message, endpoint);
        return new AsyncResult<>(null);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    @Override
    public synchronized void onApplicationEvent(EventFinishNFVO event) {
        VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = event.getEventNFVO().getVirtualNetworkFunctionRecord();
        publishEvent(event.getEventNFVO().getAction(), virtualNetworkFunctionRecord);
        if ((event.getEventNFVO().getAction().ordinal() != Action.ALLOCATE_RESOURCES.ordinal()) && (event.getEventNFVO().getAction().ordinal() != Action.GRANT_OPERATION.ordinal())) {
            findAndSetNSRStatus(virtualNetworkFunctionRecord);
        }
    }

    @Override
    public Map<String, Map<String, Integer>> getVnfrNames() {
        return vnfrNames;
    }

    @Override
    public void removeVnfrName(String nsdId, String vnfrName) {
        vnfrNames.get(nsdId).remove(vnfrName);
        if (vnfrNames.get(nsdId).size() == 0)
            vnfrNames.remove(nsdId);
    }

    @Override
    public void terminate(VirtualNetworkFunctionRecord virtualNetworkFunctionRecord) {
        ReleaseresourcesTask task = (ReleaseresourcesTask) context.getBean("releaseresourcesTask");
        task.setAction(Action.RELEASE_RESOURCES);
        task.setVirtualNetworkFunctionRecord(virtualNetworkFunctionRecord);

        this.asyncExecutor.submit(task);
    }
}
