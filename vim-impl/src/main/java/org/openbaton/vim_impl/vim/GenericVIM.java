/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.openbaton.vim_impl.vim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import org.openbaton.catalogue.mano.common.DeploymentFlavour;
import org.openbaton.catalogue.mano.common.Ip;
import org.openbaton.catalogue.mano.descriptor.VNFComponent;
import org.openbaton.catalogue.mano.descriptor.VNFDConnectionPoint;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.record.VNFCInstance;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Quota;
import org.openbaton.catalogue.nfvo.Server;
import org.openbaton.catalogue.nfvo.images.BaseNfvImage;
import org.openbaton.catalogue.nfvo.images.NFVImage;
import org.openbaton.catalogue.nfvo.networks.BaseNetwork;
import org.openbaton.catalogue.nfvo.networks.Network;
import org.openbaton.catalogue.nfvo.networks.Subnet;
import org.openbaton.catalogue.nfvo.viminstances.BaseVimInstance;
import org.openbaton.catalogue.nfvo.viminstances.OpenstackVimInstance;
import org.openbaton.catalogue.security.Key;
import org.openbaton.exceptions.PluginException;
import org.openbaton.exceptions.VimDriverException;
import org.openbaton.exceptions.VimException;
import org.openbaton.nfvo.common.utils.viminstance.VimInstanceUtils;
import org.openbaton.nfvo.vim_interfaces.vim.Vim;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

/** Created by lto on 06/04/16. */
@Service
@Scope(value = "prototype")
public class GenericVIM extends Vim {

  public GenericVIM(
      String type,
      String username,
      String password,
      String brokerIp,
      int port,
      String virtualHost,
      String managementPort,
      ApplicationContext context,
      String pluginName,
      int pluginTimeout)
      throws PluginException {
    super(
        type,
        username,
        password,
        brokerIp,
        virtualHost,
        managementPort,
        context,
        pluginName,
        pluginTimeout,
        port);
  }

  public GenericVIM() {}

  @Override
  public DeploymentFlavour add(BaseVimInstance vimInstance, DeploymentFlavour deploymentFlavour)
      throws VimException {
    try {
      log.debug(
          "Adding DeploymentFlavour with name "
              + deploymentFlavour.getFlavour_key()
              + " to VimInstance "
              + vimInstance.getName());
      DeploymentFlavour flavor = client.addFlavor(vimInstance, deploymentFlavour);
      log.info(
          "Added Flavor with name: "
              + deploymentFlavour.getFlavour_key()
              + " to VimInstance "
              + vimInstance.getName());
      return flavor;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not added Flavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not added Flavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not added Image with name: "
              + deploymentFlavour.getFlavour_key()
              + " successfully to VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public void delete(BaseVimInstance vimInstance, DeploymentFlavour deploymentFlavour)
      throws VimException {
    boolean isDeleted;
    try {
      log.debug(
          "Deleting DeploymentFlavor with name "
              + deploymentFlavour.getFlavour_key()
              + " from VimInstance "
              + vimInstance.getName());
      isDeleted = client.deleteFlavor(vimInstance, deploymentFlavour.getExtId());
      if (isDeleted) {
        log.info(
            "Deleted DeploymentFlavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " from VimInstance "
                + vimInstance.getName());
      } else {
        log.error(
            "Not deleted DeploymentFlavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully from VimInstance "
                + vimInstance.getName());
        throw new VimException(
            "Not deleted Flavor with id: "
                + deploymentFlavour.getFlavour_key()
                + " successfully from VimInstance "
                + vimInstance.getName());
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not deleted DeploymentFlavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully from VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not deleted DeploymentFlavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully from VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not deleted DeploymentFlavor with name: "
              + deploymentFlavour.getFlavour_key()
              + " successfully from VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public DeploymentFlavour update(BaseVimInstance vimInstance, DeploymentFlavour deploymentFlavour)
      throws VimException {
    try {
      log.debug(
          "Updating DeploymentFlavour with name "
              + deploymentFlavour.getFlavour_key()
              + " on VimInstance "
              + vimInstance.getName());
      DeploymentFlavour flavor = client.updateFlavor(vimInstance, deploymentFlavour);
      log.info(
          "Updated Flavor with name: "
              + deploymentFlavour.getId()
              + " on VimInstance "
              + vimInstance.getName());
      return flavor;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not updated Flavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not updated Flavor with name: "
                + deploymentFlavour.getFlavour_key()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not updated Flavor with name: "
              + deploymentFlavour.getFlavour_key()
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public List<DeploymentFlavour> queryDeploymentFlavors(BaseVimInstance vimInstance)
      throws VimException {
    try {
      log.debug("Listing DeploymentFlavors of VimInstance " + vimInstance.getName());
      List<DeploymentFlavour> flavors = client.listFlavors(vimInstance);
      log.info("Listed DeploymentFlavors of VimInstance " + vimInstance.getName());
      for (DeploymentFlavour flavour : flavors) log.debug("\t" + flavour.getFlavour_key());
      return flavors;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not listed DeploymentFlavors successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not listed DeploymentFlavors successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not listed DeploymentFlavors successfully of VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public NFVImage add(BaseVimInstance vimInstance, NFVImage image, byte[] imageFile)
      throws VimException {
    try {
      log.debug(
          "Adding image with name: "
              + image.getName()
              + " to VimInstance "
              + vimInstance.getName()
              + " using passed image file");
      NFVImage addedImage = client.addImage(vimInstance, image, imageFile);
      log.info(
          "Added Image with name: " + image.getName() + " to VimInstance " + vimInstance.getName());
      return addedImage;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not added Image with name: "
                + image.getName()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not added Image with name: "
                + image.getName()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not added Image with name: "
              + image.getName()
              + " successfully to VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public NFVImage add(BaseVimInstance vimInstance, NFVImage image, String image_url)
      throws VimException {
    try {
      log.debug(
          "Adding image with name: "
              + image.getName()
              + " to VimInstance "
              + vimInstance.getName()
              + " using image_url: "
              + image_url);
      NFVImage addedImage = client.addImage(vimInstance, image, image_url);
      log.info(
          "Added Image with name: " + image.getName() + " to VimInstance " + vimInstance.getName());
      return addedImage;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not added Image with name: "
                + image.getName()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not added Image with name: "
                + image.getName()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not added Image with name: "
              + image.getName()
              + " successfully to VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public void delete(BaseVimInstance vimInstance, NFVImage image) throws VimException {
    boolean isDeleted;
    try {
      log.debug(
          "Deleting image with name: "
              + image.getName()
              + " on VimInstance "
              + vimInstance.getName());
      isDeleted = client.deleteImage(vimInstance, image);
      if (isDeleted) {
        log.info(
            "Deleted Image with name: "
                + image.getName()
                + " on VimInstance "
                + vimInstance.getName());
      } else {
        log.warn(
            "Not deleted Image with name: "
                + image.getName()
                + " successfully on VimInstance "
                + vimInstance.getName());
        throw new VimException(
            "Not deleted Image with id: "
                + image.getId()
                + " successfully on VimInstance "
                + vimInstance.getName());
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not deleted Image with name: "
                + image.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not deleted Image with name: "
                + image.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not deleted Image with name: "
              + image.getName()
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public BaseNfvImage update(BaseVimInstance vimInstance, NFVImage image) throws VimException {
    try {
      log.debug(
          "Updating image with name: "
              + image.getName()
              + " on VimInstance "
              + vimInstance.getName());
      BaseNfvImage updatedImage = client.updateImage(vimInstance, image);
      log.info(
          "Updated Image with name: "
              + image.getName()
              + " on VimInstance "
              + vimInstance.getName());
      return updatedImage;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not updated Image with name: "
                + image.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not updated Image with name: "
                + image.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not updated Image with name: "
              + image.getName()
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public List<BaseNfvImage> queryImages(BaseVimInstance vimInstance) throws VimException {
    log.debug("Listing all Images of VimInstance " + vimInstance.getName());
    try {
      log.trace("Client is: " + client);

      List<BaseNfvImage> images = client.listImages(vimInstance);

      log.info("Listed Images of VimInstance " + vimInstance.getName());
      return images;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not listed Images successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not listed Images successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not listed Images successfully of VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public void copy(BaseVimInstance vimInstance, NFVImage image, byte[] imageFile)
      throws VimException {
    try {
      log.debug(
          "Copying image with name "
              + image.getName()
              + " to VimInstance "
              + vimInstance.getName()
              + " using image file");
      client.copyImage(vimInstance, image, imageFile);
      log.info(
          "Copied Image with name: "
              + image.getName()
              + " to VimInstance "
              + vimInstance.getName());
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not copied Image with name: "
                + image.getName()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not copied Image with name: "
                + image.getName()
                + " successfully to VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not copied Image with name: "
              + image.getName()
              + " successfully to VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public BaseNetwork add(BaseVimInstance vimInstance, BaseNetwork network) throws VimException {
    BaseNetwork createdNetwork;
    try {
      log.debug(
          "Creating Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName());
      createdNetwork = client.createNetwork(vimInstance, network);
      log.info(
          "Created Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName());
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not created Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not created Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not created Network with name: "
              + network.getName()
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
    if (network instanceof Network) {
      Network osNetowork = (Network) network;
      log.debug(
          "Creating Subnets for Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName()
              + " -> Subnets: "
              + osNetowork.getSubnets());
      Set<Subnet> createdSubnets = new HashSet<>();
      for (Subnet subnet : osNetowork.getSubnets()) {
        try {
          log.debug(
              "Creating Subnet with name: "
                  + subnet.getName()
                  + " on Network with name: "
                  + network.getName()
                  + " on VimInstance "
                  + vimInstance.getName());
          Subnet createdSubnet = client.createSubnet(vimInstance, createdNetwork, subnet);
          log.info(
              "Created Subnet with name: "
                  + subnet.getName()
                  + " on Network with name: "
                  + network.getName()
                  + " on VimInstance "
                  + vimInstance.getName());
          createdSubnet.setNetworkId(createdNetwork.getId());
          createdSubnets.add(createdSubnet);
        } catch (Exception e) {
          if (log.isDebugEnabled()) {
            log.error(
                "Not created Subnet with name: "
                    + subnet.getName()
                    + " successfully on Network with name: "
                    + network.getName()
                    + " on VimInstnace "
                    + vimInstance.getName()
                    + ". Caused by: "
                    + e.getMessage(),
                e);
          } else {
            log.error(
                "Not created Subnet with name: "
                    + subnet.getName()
                    + " successfully on Network with name: "
                    + network.getName()
                    + " on VimInstnace "
                    + vimInstance.getName()
                    + ". Caused by: "
                    + e.getMessage());
          }
          throw new VimException(
              "Not created Subnet with name: "
                  + subnet.getName()
                  + " successfully on Network with name: "
                  + network.getName()
                  + " on VimInstnace "
                  + vimInstance.getName()
                  + ". Caused by: "
                  + e.getMessage(),
              e);
        }
      }

      ((Network) createdNetwork).setSubnets(createdSubnets);
      log.info(
          "Created Subnets on Network with name: "
              + network.getName()
              + " on VimInstnace "
              + vimInstance.getName()
              + " -> Subnets: "
              + osNetowork.getSubnets());
    }
    return createdNetwork;
  }

  @Override
  public void delete(BaseVimInstance vimInstance, BaseNetwork network) throws VimException {
    boolean isDeleted;
    try {
      log.debug(
          "Deleting Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName());
      isDeleted = client.deleteNetwork(vimInstance, network.getExtId());
      if (isDeleted) {
        log.info(
            "Deleted Network with name: "
                + network.getName()
                + " on VimInstance "
                + vimInstance.getName());
      } else {
        log.error(
            "Not deleted Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName());
        throw new VimException(
            "Not deleted Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName());
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not deleted Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not deleted Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not deleted Network with name: "
              + network.getName()
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public List<BaseNetwork> queryNetwork(BaseVimInstance vimInstance) throws VimException {
    try {
      log.debug("Listing all Networks of VimInstance " + vimInstance.getName());
      List<BaseNetwork> networks = client.listNetworks(vimInstance);
      log.info("Listed Networks of VimInstance " + vimInstance.getName());
      for (BaseNetwork network : networks) log.debug("\t" + network.getName());
      return networks;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not listed Networks successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not listed Networks successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not listed Networks successfully of VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public BaseNetwork query(BaseVimInstance vimInstance, String extId) throws VimException {
    try {
      log.debug(
          "Finding Network with extId: " + extId + " on VimInstance " + vimInstance.getName());
      BaseNetwork network = client.getNetworkById(vimInstance, extId);
      log.info(
          "Found Network with extId: "
              + network.getId()
              + " on VimInstance "
              + vimInstance.getName()
              + " -> Network: "
              + network);
      return network;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not found Network with extId: "
                + extId
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not found Network with extId: "
                + extId
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not found Network with extId: "
              + extId
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  protected String getFlavorExtID(String key, OpenstackVimInstance vimInstance)
      throws VimException {
    log.debug(
        "Finding DeploymentFlavor with name: " + key + " on VimInstance " + vimInstance.getName());
    for (DeploymentFlavour deploymentFlavour : vimInstance.getFlavours()) {
      if (deploymentFlavour.getFlavour_key().equals(key)
          || deploymentFlavour.getExtId().equals(key)
          || deploymentFlavour.getId().equals(key)) {
        log.info(
            "Found DeploymentFlavour with ExtId: "
                + deploymentFlavour.getExtId()
                + " of DeploymentFlavour with name: "
                + key
                + " on VimInstance "
                + vimInstance.getName());
        return deploymentFlavour.getExtId();
      }
    }
    log.error(
        "Not found DeploymentFlavour with name: "
            + key
            + " on VimInstance "
            + vimInstance.getName());
    throw new VimException(
        "Not found DeploymentFlavour with name: "
            + key
            + " on VimInstance "
            + vimInstance.getName());
  }

  protected String chooseImage(Collection<String> vmImages, BaseVimInstance vimInstance)
      throws VimException {
    log.debug("Choosing Image...");
    log.debug("Requested: " + vmImages);

    if (vmImages != null && !vmImages.isEmpty()) {
      for (String image : vmImages) {
        Collection<BaseNfvImage> imagesByName =
            VimInstanceUtils.findActiveImagesByName(vimInstance, image);
        if (imagesByName.size() > 0) {
          //TODO implement choose
          return imagesByName.iterator().next().getExtId();
        }
      }
      throw new VimException(
          "Not found any Image with name: "
              + vmImages
              + " on VimInstance "
              + vimInstance.getName());
    }
    throw new VimException("No Images are available on VimInstnace " + vimInstance.getName());
  }

  @Override
  public List<Server> queryResources(BaseVimInstance vimInstance) throws VimException {
    log.debug("Listing all VMs of VimInstance " + vimInstance.getName());
    try {
      List<Server> servers = client.listServer(vimInstance);
      log.trace("Listed VMs of VimInstance " + vimInstance.getName() + " -> VMs: " + servers);
      return servers;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not listed VMs successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not listed VMs successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not listed VMs successfully of VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
  }

  @Override
  public void update(VirtualDeploymentUnit vdu) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void scale(VirtualDeploymentUnit vdu) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void migrate(VirtualDeploymentUnit vdu) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void operate(VirtualDeploymentUnit vdu, String operation) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  @Async
  public Future<Void> release(VNFCInstance vnfcInstance, BaseVimInstance vimInstance)
      throws VimException {
    log.debug(
        "Removing VM with ExtId: "
            + vnfcInstance.getVc_id()
            + " from VimInstance "
            + vimInstance.getName());
    try {
      client.deleteServerByIdAndWait(vimInstance, vnfcInstance.getVc_id());
      log.info(
          "Removed VM with ExtId: "
              + vnfcInstance.getVc_id()
              + " from VimInstance "
              + vimInstance.getName());
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not removed VM with ExtId "
                + vnfcInstance.getVc_id()
                + " successfully from VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not removed VM with ExtId "
                + vnfcInstance.getVc_id()
                + " successfully from VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not removed VM with ExtId "
              + vnfcInstance.getVc_id()
              + " successfully from VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
    return new AsyncResult<>(null);
  }

  @Override
  public void createReservation(VirtualDeploymentUnit vdu) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void queryReservation() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void updateReservation(VirtualDeploymentUnit vdu) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void releaseReservation(VirtualDeploymentUnit vdu) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Quota getQuota(BaseVimInstance vimInstance) throws VimException {
    log.debug("Listing Quota for Tenant of VimInstance " + vimInstance.getName());
    Quota quota;
    try {
      quota = client.getQuota(vimInstance);
      log.info(
          "Listed Quota successfully for Tenant of VimInstance "
              + vimInstance.getName()
              + " -> Quota: "
              + quota);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not listed Quota successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not listed Quota successfully for Tenant of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not listed Quota successfully for Tenant of VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
    return quota;
  }

  @Override
  public BaseVimInstance refresh(BaseVimInstance vimInstance) throws VimException {
    try {
      return client.refresh(vimInstance);
    } catch (VimDriverException e) {
      throw new VimException(
          "Vim instance Not refreshed " + vimInstance.getName() + ". Caused by: " + e.getMessage(),
          e);
    }
  }

  @Override
  public BaseNetwork update(BaseVimInstance vimInstance, BaseNetwork network) throws VimException {
    BaseNetwork updatedNetwork;
    try {
      log.debug(
          "Updating Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName());
      updatedNetwork = client.updateNetwork(vimInstance, network);
      log.info(
          "Updated Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName());
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not updated Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not updated Network with name: "
                + network.getName()
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      throw new VimException(
          "Not updated Network with name: "
              + network.getName()
              + " successfully on VimInstance "
              + vimInstance.getName()
              + ". Caused by: "
              + e.getMessage(),
          e);
    }
    if (network instanceof Network) {
      log.debug(
          "Updating Subnets for Network with name: "
              + network.getName()
              + " on VimInstance "
              + vimInstance.getName()
              + " -> "
              + ((Network) network).getSubnets());
      Set<Subnet> updatedSubnets = new HashSet<>();
      List<String> updatedSubnetExtIds = new ArrayList<>();
      for (Subnet subnet : ((Network) network).getSubnets()) {
        if (subnet.getExtId() != null) {
          try {
            log.debug(
                "Updating Subnet with name: "
                    + subnet.getName()
                    + " on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName());
            Subnet updatedSubnet = client.updateSubnet(vimInstance, updatedNetwork, subnet);
            log.info(
                "Updated Subnet with name: "
                    + subnet.getName()
                    + " on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName());
            updatedSubnet.setNetworkId(updatedNetwork.getId());
            updatedSubnets.add(updatedSubnet);
            updatedSubnetExtIds.add(updatedSubnet.getExtId());
          } catch (Exception e) {
            if (log.isDebugEnabled()) {
              log.error(
                  "Not updated Subnet with name: "
                      + subnet.getName()
                      + " successfully on Network with name: "
                      + network.getName()
                      + " on VimInstance "
                      + vimInstance.getName()
                      + ". Caused by: "
                      + e.getMessage(),
                  e);
            } else {
              log.error(
                  "Not updated Subnet with name: "
                      + subnet.getName()
                      + " successfully on Network with name: "
                      + network.getName()
                      + " on VimInstance "
                      + vimInstance.getName()
                      + ". Caused by: "
                      + e.getMessage());
            }
            throw new VimException(
                "Not updated Subnet with name: "
                    + subnet.getName()
                    + " successfully on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName()
                    + ". Caused by: "
                    + e.getMessage(),
                e);
          }
        } else {
          try {
            log.debug(
                "Creating Subnet with name: "
                    + subnet.getName()
                    + " on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName());
            Subnet createdSubnet = client.createSubnet(vimInstance, updatedNetwork, subnet);
            log.info(
                "Created Subnet with name: "
                    + subnet.getName()
                    + " on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName());
            createdSubnet.setNetworkId(updatedNetwork.getId());
            updatedSubnets.add(createdSubnet);
            updatedSubnetExtIds.add(createdSubnet.getExtId());
          } catch (Exception e) {
            if (log.isDebugEnabled()) {
              log.error(
                  "Not created Subnet with name: "
                      + subnet.getName()
                      + " successfully on Network with name: "
                      + network.getName()
                      + " on VimInstance "
                      + vimInstance.getName()
                      + ". Caused by: "
                      + e.getMessage(),
                  e);
            } else {
              log.error(
                  "Not created Subnet with name: "
                      + subnet.getName()
                      + " successfully on Network with name: "
                      + network.getName()
                      + " on VimInstance "
                      + vimInstance.getName()
                      + ". Caused by: "
                      + e.getMessage());
            }
            throw new VimException(
                "Not created Subnet with name: "
                    + subnet.getName()
                    + " successfully on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName()
                    + ". Caused by: "
                    + e.getMessage(),
                e);
          }
        }
      }
      ((Network) updatedNetwork).setSubnets(updatedSubnets);
      List<String> existingSubnetExtIds;
      try {
        log.debug(
            "Listing all Subnet IDs of Network with name: "
                + network.getName()
                + " on VimInstance "
                + vimInstance.getName());
        existingSubnetExtIds = client.getSubnetsExtIds(vimInstance, updatedNetwork.getExtId());
        log.info(
            "Listed all Subnet IDs of Network with name: "
                + network.getName()
                + " on VimInstance "
                + vimInstance.getName()
                + " -> Subnet IDs: "
                + existingSubnetExtIds);
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error(
              "Not listed Subnets of Network with name: "
                  + network.getName()
                  + " successfully of VimInstance "
                  + vimInstance.getName()
                  + ". Caused by: "
                  + e.getMessage(),
              e);
        } else {
          log.error(
              "Not listed Subnets of Network with name: "
                  + network.getName()
                  + " successfully of VimInstance "
                  + vimInstance.getName()
                  + ". Caused by: "
                  + e.getMessage());
        }
        throw new VimException(
            "Not listed Subnets of Network with name: "
                + network.getName()
                + " successfully of VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      }
      for (String existingSubnetExtId : existingSubnetExtIds) {
        if (!updatedSubnetExtIds.contains(existingSubnetExtId)) {
          try {
            log.debug(
                "Deleting Subnet with id: "
                    + existingSubnetExtId
                    + " on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName());
            client.deleteSubnet(vimInstance, existingSubnetExtId);
            log.info(
                "Deleted Subnet with id: "
                    + existingSubnetExtId
                    + " on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName());
          } catch (Exception e) {
            if (log.isDebugEnabled()) {
              log.error(
                  "Not Deleted Subnet with id: "
                      + existingSubnetExtId
                      + " successfully on Network with name: "
                      + network.getName()
                      + " on VimInstance "
                      + vimInstance.getName()
                      + ". Caused by: "
                      + e.getMessage(),
                  e);
            } else {
              log.error(
                  "Not Deleted Subnet with id: "
                      + existingSubnetExtId
                      + " successfully on Network with name: "
                      + network.getName()
                      + " on VimInstance "
                      + vimInstance.getName()
                      + ". Caused by: "
                      + e.getMessage());
            }
            throw new VimException(
                "Not Deleted Subnet with id: "
                    + existingSubnetExtId
                    + " successfully on Network with name: "
                    + network.getName()
                    + " on VimInstance "
                    + vimInstance.getName()
                    + ". Caused by: "
                    + e.getMessage(),
                e);
          }
        }
      }
    }
    log.info(
        "Subnets of Network with name: "
            + network.getName()
            + " updated successfully on VimInstance "
            + vimInstance.getName());
    return updatedNetwork;
  }

  @Override
  @Async
  public Future<VNFCInstance> allocate(
      BaseVimInstance vimInstance,
      VirtualDeploymentUnit vdu,
      VirtualNetworkFunctionRecord vnfr,
      VNFComponent vnfComponent,
      String userdata,
      Map<String, String> floatingIps,
      Set<Key> keys)
      throws VimException {
    log.debug("Launching new VM on VimInstance: " + vimInstance.getName());
    log.debug("VDU is : " + vdu.getName());
    log.debug("VNFR is : " + vnfr.getName());
    log.debug("VNFC is : " + vnfComponent.toString());
    /* *) choose image *) ...? */
    String image = this.chooseImage(vdu.getVm_image(), vimInstance);

    log.debug("Finding Networks...");
    Set<VNFDConnectionPoint> networks = new HashSet<>(vnfComponent.getConnection_point());
    String flavorKey;
    if (vdu.getComputation_requirement() != null && !vdu.getComputation_requirement().isEmpty()) {
      flavorKey = vdu.getComputation_requirement();
    } else {
      flavorKey = vnfr.getDeployment_flavour_key();
    }
    String flavorExtId;
    if (vimInstance instanceof OpenstackVimInstance)
      flavorExtId = getFlavorExtID(flavorKey, (OpenstackVimInstance) vimInstance);
    else flavorExtId = "";

    log.debug("Generating Hostname...");
    vdu.setHostname(vnfr.getName());
    String hostname = vdu.getHostname() + "-" + ((int) (Math.random() * 10000000));
    log.debug("Generated Hostname: " + hostname);

    userdata = userdata.replace("#Hostname=", "Hostname=" + hostname);

    Set<String> securityGroups = null;

    Server server = null;
    VNFCInstance vnfcInstance = null;
    try {
      if (image == null) throw new NullPointerException("image is null");
      if (flavorExtId == null) throw new NullPointerException("flavorExtId is null");
      String keyPair = "";
      if (vimInstance instanceof OpenstackVimInstance) {
        if (((OpenstackVimInstance) vimInstance).getKeyPair() == null) {
          log.debug("vimInstance.getKeyPair() is null");
          keyPair = "";
        } else {
          keyPair = ((OpenstackVimInstance) vimInstance).getKeyPair();
        }
      }
      if (networks.isEmpty()) {
        throw new NullPointerException("networks is empty");
      }
      if (vimInstance instanceof OpenstackVimInstance) {
        if (((OpenstackVimInstance) vimInstance).getSecurityGroups() == null) {
          securityGroups = new HashSet<>();
        } else securityGroups = ((OpenstackVimInstance) vimInstance).getSecurityGroups();
        if (vdu.getMetadata() != null && vdu.getMetadata().containsKey("az")) {
          if (vimInstance.getMetadata() == null) vimInstance.setMetadata(new HashMap<>());
          vimInstance.getMetadata().put("az", vdu.getMetadata().get("az"));
        }
      }
      log.debug("Using SecurityGroups: " + securityGroups);
      log.debug(
          "Launching VM with params: "
              + hostname
              + " - "
              + image
              + " - "
              + flavorExtId
              + " - "
              + keyPair
              + " - "
              + networks
              + " - "
              + securityGroups);
      server =
          client.launchInstanceAndWait(
              vimInstance,
              hostname,
              image,
              flavorExtId,
              keyPair,
              vnfComponent.getConnection_point(),
              securityGroups,
              userdata,
              floatingIps,
              new HashSet<>(keys));
      log.debug(
          "Launched VM with hostname "
              + hostname
              + " with ExtId "
              + server.getExtId()
              + " on VimInstance "
              + vimInstance.getName());
    } catch (VimDriverException e) {
      if (log.isDebugEnabled()) {
        log.error(
            "Not launched VM with hostname "
                + hostname
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e);
      } else {
        log.error(
            "Not launched VM with hostname "
                + hostname
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage());
      }
      VimDriverException vimDriverException = (VimDriverException) e.getCause();
      if (vimDriverException != null && vimDriverException.getServer() != null) {
        server = vimDriverException.getServer();
        try {
          vnfcInstance =
              getVnfcInstance(vimInstance, vnfComponent, hostname, server, vdu, floatingIps, vnfr);
        } catch (VimDriverException | VimException e1) {
          throw new VimException(e);
        }
        throw new VimException(
            "Not launched VM with hostname "
                + hostname
                + " successfully on VimInstance "
                + vimInstance.getName()
                + ". Caused by: "
                + e.getMessage(),
            e,
            vdu,
            vnfcInstance);
      } else {
        try {
          log.warn(
              "Exception thrown while deploying... Try to recover '"
                  + hostname
                  + "' from VIM directly");
          vnfcInstance =
              getVnfcInstance(vimInstance, vnfComponent, hostname, null, vdu, floatingIps, vnfr);
          //checkIntegrity(vnfr, vdu, vnfComponent, vnfcInstance, null);
        } catch (VimDriverException | VimException e1) {
          if ((e1 instanceof VimException) && ((VimException) e1).getVnfcInstance() != null)
            vnfcInstance = ((VimException) e1).getVnfcInstance();
          else {
            vnfcInstance = new VNFCInstance();
            vnfcInstance.setHostname(hostname);
            vnfcInstance.setVim_id(vimInstance.getId());
            vnfcInstance.setVnfComponent(vnfComponent);
            vnfcInstance.setVc_id("unknown");
            vnfcInstance.setState("ERROR");
            vnfcInstance.setIps(new HashSet<>());
            vnfcInstance.setFloatingIps(new HashSet<>());
          }
          throw new VimException(
              "Not launched VM with hostname "
                  + hostname
                  + " successfully on VimInstance "
                  + vimInstance.getName()
                  + ". Caused by: "
                  + e.getMessage(),
              e,
              vdu,
              vnfcInstance);
        }
      }
    }
    if (vnfcInstance == null) {
      try {
        log.debug("Creating VNFCInstance based on the VM launched previously -> VM: " + server);
        vnfcInstance =
            getVnfcInstance(vimInstance, vnfComponent, hostname, server, vdu, floatingIps, vnfr);
        //checkIntegrity(vnfr, vdu, vnfComponent, vnfcInstance, server);
      } catch (VimDriverException | VimException e) {
        throw new VimException(e);
      }
    }

    log.info("Launched VNFCInstance: " + vnfcInstance + " on VimInstance " + vimInstance.getName());
    return new AsyncResult<>(vnfcInstance);
  }

  private VNFCInstance getVnfcInstance(
      BaseVimInstance vimInstance,
      VNFComponent vnfComponent,
      String hostname,
      Server server,
      VirtualDeploymentUnit vdu,
      Map<String, String> floatingIps,
      VirtualNetworkFunctionRecord vnfr)
      throws VimDriverException, VimException {
    VNFCInstance vnfcInstance = new VNFCInstance();
    if (server == null) {
      log.trace("Listing potential VMs to recover...");
      List<Server> serversFromVim = client.listServer(vimInstance);
      log.debug("Listed potential VMs to recover -> " + serversFromVim);
      for (Server serverFromVim : serversFromVim) {
        if (serverFromVim.getHostName().equals(hostname)) {
          server = serverFromVim;
          log.debug("Found VM -> " + server);
          break;
        }
      }
      if (server == null) {
        throw new VimException(
            "Unable to recover VNFCInstance from VIM. Probably was not launched at all...");
      }
    }
    vnfcInstance.setHostname(hostname);
    if (server.getExtId() != null) {
      vnfcInstance.setVc_id(server.getExtId());
    } else {
      vnfcInstance.setVc_id("unknown");
    }
    vnfcInstance.setVim_id(vimInstance.getId());
    vnfcInstance.setState(server.getStatus());

    vnfcInstance.setConnection_point(new HashSet<>());

    for (VNFDConnectionPoint connectionPoint : vnfComponent.getConnection_point()) {
      VNFDConnectionPoint connectionPoint_vnfci = new VNFDConnectionPoint();
      connectionPoint_vnfci.setVirtual_link_reference(connectionPoint.getVirtual_link_reference());
      connectionPoint_vnfci.setType(connectionPoint.getType());
      if (server.getFloatingIps() != null)
        for (Entry<String, String> entry : server.getFloatingIps().entrySet())
          if (entry.getKey().equals(connectionPoint.getVirtual_link_reference()))
            connectionPoint_vnfci.setFloatingIp(entry.getValue());
      vnfcInstance.getConnection_point().add(connectionPoint_vnfci);
    }

    if (vdu.getVnfc_instance() == null) vdu.setVnfc_instance(new HashSet<>());

    vnfcInstance.setVnfComponent(vnfComponent);

    vnfcInstance.setIps(new HashSet<>());
    vnfcInstance.setFloatingIps(new HashSet<>());

    if (!floatingIps.isEmpty()) {
      for (Entry<String, String> fip : server.getFloatingIps().entrySet()) {
        Ip ip = new Ip();
        ip.setNetName(fip.getKey());
        ip.setIp(fip.getValue());
        vnfcInstance.getFloatingIps().add(ip);
      }
    }

    for (Entry<String, List<String>> network : server.getIps().entrySet()) {
      Ip ip = new Ip();
      ip.setNetName(network.getKey());
      ip.setIp(network.getValue().iterator().next());
      vnfcInstance.getIps().add(ip);
      for (String ip1 : server.getIps().get(network.getKey())) {
        vnfr.getVnf_address().add(ip1);
      }
    }
    return vnfcInstance;
  }

  public void checkIntegrity(
      VirtualNetworkFunctionRecord vnfr,
      VirtualDeploymentUnit vdu,
      VNFComponent vnfComponent,
      VNFCInstance vnfcInstance,
      Server server)
      throws VimException {
    try {
      if (vnfComponent.getConnection_point().size() != vnfcInstance.getIps().size()) {
        throw new VimException(
            "Not all (or too many) internal IPs were associated. Expected: "
                + vnfComponent.getConnection_point().size()
                + " Allocated: "
                + vnfcInstance.getIps().size());
      }
      int expectedFloatingIpCount = 0;
      for (VNFDConnectionPoint cp : vnfComponent.getConnection_point()) {
        if (cp.getFloatingIp() != null && !cp.getFloatingIp().isEmpty()) {
          expectedFloatingIpCount++;
        }
      }
      if (expectedFloatingIpCount != vnfcInstance.getFloatingIps().size()) {
        throw new VimException(
            "Not all (or too many) Floating IPs were associated. Expected: "
                + expectedFloatingIpCount
                + " Allocated: "
                + vnfcInstance.getFloatingIps().size());
      }
      if (!vdu.getVm_image().contains(server.getImage().getName())) {
        throw new VimException(
            "Server launched with incorrect image. Expected: "
                + vdu.getVm_image()
                + " Used: "
                + server.getImage().getName());
      }
      if (!server.getFlavor().getFlavour_key().equals(vnfr.getDeployment_flavour_key())) {
        throw new VimException(
            "Server launched with incorrect flavor. Expected: "
                + vnfr.getDeployment_flavour_key()
                + " Used: "
                + server.getFlavor().getFlavour_key());
      }
    } catch (Exception e) {
      throw new VimException(
          "VNFCInstance was not deployed correctly -> " + e.getMessage(), e, vdu, vnfcInstance);
    }
  }
}
