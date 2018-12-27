package nl.nlighten.prometheus.wildfly;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Exports infinispan metrics.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new InfinispanExports().register();
 * }
 * </pre>
 * <p>
 * Example metrics being exported:
 * <pre>
 *   infinispan_hit_ratio{name="myReplicatedCache(repl_sync)",manager="myCacheContainer",} 0.93
 *   infinispan_hit_total{name="myReplicatedCache(repl_sync)",manager="myCacheContainer",} 930.0
 *   infinispan_miss_total{name="myReplicatedCache(repl_sync)",manager="myCacheContainer",} 70.0
 *   infinispan_entries_total{myReplicatedCache(repl_sync)="foo",manager="myCacheContainer",} 1021.0
 *   infinispan_evictions_total{myReplicatedCache(repl_sync)="foo",manager="myCacheContainer",} 0.0
 * </pre>
 */
public class InfinispanExports extends Collector {

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName filterName = new ObjectName("org.wildfly.clustering.infinispan:component=Statistics,name=*,manager=*,type=Cache");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);
            if (mBeans.size() > 0) {

                GaugeMetricFamily hitRatioGauge = new GaugeMetricFamily(
                        "infinispan_hit_ratio",
                        "Cache hit ratio",
                        Arrays.asList("name", "manager"));

                CounterMetricFamily hitsCounter = new CounterMetricFamily(
                        "infinispan_hit_total",
                        "Number of hits",
                        Arrays.asList("name", "manager"));

                CounterMetricFamily missesCounter = new CounterMetricFamily(
                        "infinispan_miss_total",
                        "Number of misses",
                        Arrays.asList("name", "manager"));

                GaugeMetricFamily numberOfEntriesGauge = new GaugeMetricFamily(
                        "infinispan_entries_total",
                        "Number of entries",
                        Arrays.asList("name", "manager"));

                CounterMetricFamily evictionsCounter = new CounterMetricFamily(
                        "infinispan_evictions_total",
                        "Number of evictions",
                        Arrays.asList("name", "manager"));

                for (final ObjectInstance mBean : mBeans) {

                    hitRatioGauge.addMetric(
                            Arrays.asList(Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("name")), Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("manager"))),
                            (Double) server.getAttribute(mBean.getObjectName(), "hitRatio"));
                    hitsCounter.addMetric(
                            Arrays.asList(Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("name")), Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("manager"))),
                            ((Long) server.getAttribute(mBean.getObjectName(), "hits")).doubleValue());

                    missesCounter.addMetric(
                            Arrays.asList(Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("name")), Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("manager"))),
                            ((Long) server.getAttribute(mBean.getObjectName(), "misses")).doubleValue());

                    numberOfEntriesGauge.addMetric(
                            Arrays.asList(Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("name")), Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("manager"))),
                            ((Integer) server.getAttribute(mBean.getObjectName(), "numberOfEntries")).doubleValue());

                    evictionsCounter.addMetric(
                            Arrays.asList(Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("name")), Util.sanitizeLabel(mBean.getObjectName().getKeyProperty("manager"))),
                            ((Long) server.getAttribute(mBean.getObjectName(), "evictions")).doubleValue());

                }
                mfs.add(hitRatioGauge);
                mfs.add(hitsCounter);
                mfs.add(missesCounter);
                mfs.add(numberOfEntriesGauge);
                mfs.add(evictionsCounter);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mfs;
    }
}

