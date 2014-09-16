package org.ovirt.engine.core.bll.network.cluster;

import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.compat.Guid;

public interface ManagementNetworkUtil {

    /**
     * The method retrieves the management network for the given cluster.
     *
     * @param clusterId
     *            the given cluster id
     * @return {@link Network} that is defined as the management one in the given cluster
     */
    Network getManagementNetwork(Guid clusterId);

    /**
     * The method checks if the given network is defined as the management network for any cluster.
     *
     * @param networkId
     *            the given network id
     * @return true if exists a cluster where the network is defined as the management one,
     *         false otherwise
     */
    boolean isManagementNetwork(Guid networkId);

    /**
     * The method checks if the given network is defined as the management network for the given cluster.
     *
     * @param networkId
     *            the given network id
     * @param clusterId
     *            the given cluster id
     * @return true if the network is defined as the management one for the given cluster,
     *         false otherwise
     */
    boolean isManagementNetwork(Guid networkId, Guid clusterId);

}
