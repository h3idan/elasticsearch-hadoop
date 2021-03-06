/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.hadoop.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.hadoop.cfg.InternalConfigurationOptions;
import org.elasticsearch.hadoop.cfg.Settings;

public abstract class SettingsUtils {

    private static List<String> qualifyNodes(String nodes, int defaultPort) {
        List<String> list = StringUtils.tokenize(nodes);
        for (int i = 0; i < list.size(); i++) {
            String host = list.get(i);
            list.set(i, qualifyNode(host, defaultPort));
        }
        return list;
    }

    private static String qualifyNode(String node, int defaultPort) {
        return (node.contains(":") ? node : node + ":" + defaultPort);
    }

    public static void pinNode(Settings settings, String node) {
        pinNode(settings, node, settings.getPort());
    }

    public static void pinNode(Settings settings, String node, int port) {
        settings.setProperty(InternalConfigurationOptions.INTERNAL_ES_PINNED_NODE, qualifyNode(node, port));
    }

    public static boolean hasPinnedNode(Settings settings) {
        return StringUtils.hasText(settings.getProperty(InternalConfigurationOptions.INTERNAL_ES_PINNED_NODE));
    }

    public static String getPinnedNode(Settings settings) {
        String node = settings.getProperty(InternalConfigurationOptions.INTERNAL_ES_PINNED_NODE);
        Assert.hasText(node, "Task has not been pinned to a node...");
        return node;
    }

    public static void addDiscoveredNodes(Settings settings, List<String> discoveredNodes) {
        // clean-up and merge
        Set<String> nodes = new LinkedHashSet<String>();
        nodes.addAll(declaredNodes(settings));
        nodes.addAll(discoveredNodes);

        setDiscoveredNodes(settings, nodes);
    }

    public static void setDiscoveredNodes(Settings settings, Collection<String> nodes) {
        settings.setProperty(InternalConfigurationOptions.INTERNAL_ES_DISCOVERED_NODES, StringUtils.concatenate(nodes, ","));
    }

    public static List<String> declaredNodes(Settings settings) {
        return qualifyNodes(settings.getNodes(), settings.getPort());
    }

    public static List<String> discoveredOrDeclaredNodes(Settings settings) {
        // returned the discovered nodes or, if not defined, the set nodes
        String discoveredNodes = settings.getProperty(InternalConfigurationOptions.INTERNAL_ES_DISCOVERED_NODES);
        return (StringUtils.hasText(discoveredNodes) ? StringUtils.tokenize(discoveredNodes) : declaredNodes(settings));
    }

    public static Map<String, String> aliases(String definition) {
        List<String> aliases = StringUtils.tokenize(definition, ",");

        Map<String, String> aliasMap = new LinkedHashMap<String, String>();

        if (aliases != null) {
            for (String string : aliases) {
                // split alias
                string = string.trim();
                int index = string.indexOf(":");
                if (index > 0) {
                    String key = string.substring(0, index);
                    // save the lower case version as well since Hive does that for top-level keys
                    aliasMap.put(key, string.substring(index + 1));
                    aliasMap.put(key.toLowerCase(Locale.ENGLISH), string.substring(index + 1));
                }
            }
        }

        return aliasMap;
    }

    /**
     * Whether the settings indicate a ES 1.0RC1 (which introduces breaking changes) or lower (1.0.0.Beta2)
     *
     * @param settings
     * @return
     */
    public static boolean isEs10(Settings settings) {
        String version = settings.getProperty(InternalConfigurationOptions.INTERNAL_ES_VERSION);
        // assume ES 1.0 by default
        if (!StringUtils.hasText(version)) {
            return true;
        }

        return ("1.0.0.RC".compareTo(version) <= 0 || "1.0.0".equals(version));
    }
}