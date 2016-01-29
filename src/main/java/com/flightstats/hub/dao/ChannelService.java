package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

public interface ChannelService {

    boolean channelExists(String channelName);

    ChannelConfig createChannel(ChannelConfig configuration);

    ContentKey insert(String channelName, Content content) throws Exception;

    Collection<ContentKey> insert(BulkContent content) throws Exception;

    Optional<Content> getValue(Request request);

    ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache);

    ChannelConfig getCachedChannelConfig(String channelName);

    Iterable<ChannelConfig> getChannels();

    Iterable<ChannelConfig> getChannels(String tag);

    Iterable<String> getTags();

    ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig);

    SortedSet<ContentKey> queryByTime(TimeQuery timeQuery);

    SortedSet<ContentKey> getKeys(DirectionQuery query);

    boolean delete(String channelName);

    boolean isReplicating(String channelName);

    Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace);

    void deleteBefore(String name, ContentKey limitKey);

    void getValues(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback);
}
