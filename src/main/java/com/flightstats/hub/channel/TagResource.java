package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.rest.Linked;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * This resource represents operations on tags.
 */
@Path("/tag")
public class TagResource {

    @Context
    private UriInfo uriInfo;

    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannels() {
        Iterable<String> tags = channelService.getTags();
        Map<String, URI> tagUriMap = new HashMap<>();
        for (String tag : tags) {
            tagUriMap.put(tag, URI.create(uriInfo.getBaseUri() + "tag/" + tag));
        }
        Linked<?> result = LinkBuilder.buildLinks(uriInfo, tagUriMap, "tags");
        return Response.ok(result).build();
    }


}
