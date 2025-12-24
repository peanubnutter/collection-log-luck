package com.peanubnutter.collectionlogluck.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.TreeMap;

@Getter
@AllArgsConstructor
public class CollectionLog
{
    private final String username;

    @Setter
    private int totalObtained;

    @Setter
    private int totalItems;

    @Setter
    private int uniqueObtained;

    @Setter
    private int uniqueItems;

    private final Map<String, CollectionLogTab> tabs;

    private final Map<String, CollectionLogItem> collectionLogItemCache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CollectionLogKillCount> collectionLogKillCountCache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public CollectionLogPage searchForPage(String pageName)
    {
        if (StringUtils.isEmpty(pageName))
        {
            return null;
        }

        for (CollectionLogTab tab : tabs.values())
        {
            for (CollectionLogPage page : tab.getPages().values())
            {
                if (pageName.equalsIgnoreCase(page.getName()))
                {
                    return page;
                }
            }
        }
        return null;
    }

    public CollectionLogItem searchForItem(String itemName) {
        if (StringUtils.isEmpty(itemName)) {
            return null;
        }

        if (!collectionLogItemCache.containsKey(itemName)) {
            // if appearing on multiple pages, take the highest amount seen
            int highestSeen = Integer.MIN_VALUE;
            CollectionLogItem best = null;

            for (CollectionLogTab tab : tabs.values()) {
                for (CollectionLogPage page : tab.getPages().values()) {
                    for (CollectionLogItem item : page.getItems()) {
                        if (itemName.equalsIgnoreCase(item.getName())) {
                            if (item.getQuantity() > highestSeen) {
                                highestSeen = item.getQuantity();
                                best = item;
                            }
                        }
                    }
                }
            }

            collectionLogItemCache.put(itemName, best);
        }

        return collectionLogItemCache.get(itemName);
    }

    public CollectionLogKillCount searchForKillCount(String killCountName) {
        if (StringUtils.isEmpty(killCountName)) {
            return null;
        }

        if (!collectionLogKillCountCache.containsKey(killCountName)) {
            // if appearing on multiple pages, take the highest amount seen
            int highestSeen = Integer.MIN_VALUE;
            CollectionLogKillCount best = null;

            // This code is usually not necessary, but just in case a KC appears on multiple pages (might happen rarely
            // with clues or something else), take the highest value seen.
            for (CollectionLogTab tab : tabs.values()) {
                for (CollectionLogPage page : tab.getPages().values()) {
                    for (CollectionLogKillCount killCount : page.getKillCounts()) {
                        String strippedKc = Text.removeTags(killCount.getName());
                        if (killCountName.equalsIgnoreCase(strippedKc)) {
                            if (killCount.getAmount() > highestSeen) {
                                highestSeen = killCount.getAmount();
                                best = killCount;
                            }
                        }
                    }
                }
            }

//            // Uncomment this code to print any missing entries from LogItemSourceInfo list
//            LogItemSourceInfo logItemSourceInfo = LogItemSourceInfo.findByName(killCountName);
//            if (logItemSourceInfo == null) {
//                // import org.slf4j.* for these to work
//                Logger logger = LoggerFactory.getLogger(CollectionLog.class);
//                logger.error("!!!!!!!!!!New collection log page detected!: (" + killCountName + ")");
//            }

            collectionLogKillCountCache.put(killCountName, best);
        }

        return collectionLogKillCountCache.get(killCountName);
    }

}