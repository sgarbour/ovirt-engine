package org.ovirt.engine.core.vdsbroker.monitoring.kubevirt;

import java.io.IOException;
import java.util.stream.Collectors;

import org.ovirt.engine.core.common.businessentities.HugePage;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.VdsStatistics;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dao.provider.ProviderDao;
import org.ovirt.engine.core.vdsbroker.VdsManager;
import org.ovirt.engine.core.vdsbroker.kubevirt.KubevirtUtils;
import org.ovirt.engine.core.vdsbroker.kubevirt.PrometheusClient;
import org.ovirt.engine.core.vdsbroker.monitoring.HostMonitoringInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubevirtNodesMonitoring implements HostMonitoringInterface {
    private static Logger log = LoggerFactory.getLogger(KubevirtNodesMonitoring.class);
    private ProviderDao providerDao;
    private VdsManager vdsManager;
    private PrometheusClient prometheusClient;
    private AuditLogDirector auditLogDirector;

    public KubevirtNodesMonitoring(VdsManager vdsManager, ProviderDao providerDao, AuditLogDirector auditLogDirector) {
        this.vdsManager = vdsManager;
        this.providerDao = providerDao;
        this.auditLogDirector = auditLogDirector;
    }

    private PrometheusClient getPrometheusClient() {
        if (prometheusClient == null) {
            Provider<?> provider = providerDao.get(vdsManager.getClusterId());
            prometheusClient = KubevirtUtils.create(provider, auditLogDirector);
        }
        return prometheusClient;
    }

    @Override
    public void refresh() {
        String name = vdsManager.getVdsName();
        log.debug("refreshing host {}", name);

        // Update statistics:
        try {
            PrometheusClient promClient = getPrometheusClient();
            if (vdsManager.isTimeToRefreshStatistics() && promClient != null) {
                VdsStatistics stat = new VdsStatistics();
                stat.setId(vdsManager.getVdsId());

                // memory
                stat.setUsageMemPercent(promClient.getNodeMemUsage(name));
                stat.setMemAvailable(promClient.getNodeMemAvailable(name));
                stat.setMemFree(promClient.getNodeMemFree(name));
                stat.setMemShared(promClient.getNodeMemShared(name));

                // swap
                stat.setSwapTotal(promClient.getNodeSwapTotalMb(name));
                stat.setSwapFree(promClient.getNodeSwapFreeMb(name));

                // huge pages
                stat.setAnonymousHugePages(promClient.getNodeAnonHugePages(name));
                stat.setHugePages(
                        promClient.getNodeHugePages(name).stream()
                                .map(k -> new HugePage(k.getFirst(), k.getSecond()))
                                .collect(Collectors.toList())
                );

                // boot time
                stat.setBootTime(promClient.getNodeBootTime(name));

                // Cpu
                stat.setCpuIdle(promClient.getNodeCpuIdle(name));
                stat.setCpuSys(promClient.getNodeCpuSystem(name));
                stat.setCpuUser(promClient.getNodeCpuUser(name));
                stat.setCpuLoad(promClient.getNodeCpuLoad(name));
                stat.setUsageCpuPercent(promClient.getNodeCpuUsage(name));

                // TODO: KSM

                vdsManager.updateStatisticsData(stat);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        vdsManager.afterRefreshTreatment(true);
    }

    @Override
    public void afterRefreshTreatment() {
        try {
            if (prometheusClient != null) {
                prometheusClient.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
