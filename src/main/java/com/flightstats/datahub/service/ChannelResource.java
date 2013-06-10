package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.Linked;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents the collection of all channels in the DataHub.
 */
@Path("/channel")
public class ChannelResource {

	private final ChannelDao channelDao;
	private final ChannelHypermediaLinkBuilder linkBuilder;
	private final UriInfo uriInfo;
	private final CreateChannelValidator createChannelValidator;

	@Inject
	public ChannelResource(ChannelDao channelDao, ChannelHypermediaLinkBuilder linkBuilder, CreateChannelValidator createChannelValidator, UriInfo uriInfo) {
		this.channelDao = channelDao;
		this.linkBuilder = linkBuilder;
		this.uriInfo = uriInfo;
		this.createChannelValidator = createChannelValidator;
	}

	@GET
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	public Response getChannels() {
		Iterable<ChannelConfiguration> channels = channelDao.getChannels();
		Iterable<String> channelNames = Iterables.transform(channels, new Function<ChannelConfiguration, String>() {
			@Override
			public String apply(ChannelConfiguration input) {
				return input.getName();
			}
		});
		ImmutableMap<String, URI> mappedChannels = Maps.toMap(channelNames, new Function<String, URI>() {
			@Override
			public URI apply(String channelName) {
				return linkBuilder.buildChannelUri(channelName);
			}
		});

		Linked.Builder<?> responseBuilder = Linked.justLinks();
		responseBuilder.withLink("self", uriInfo.getRequestUri());

		List<HalLink> channelLinks = new ArrayList<>(mappedChannels.size());
		for (Map.Entry<String, URI> entry : mappedChannels.entrySet()) {
			HalLink link = new HalLink(entry.getKey(), entry.getValue());
			channelLinks.add(link);
		}
		responseBuilder.withLinks("channels", channelLinks);
		Linked<?> result = responseBuilder.build();
		return Response.ok(result).build();
	}

	@POST
	@Timed
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createChannel(ChannelCreationRequest channelCreationRequest) throws InvalidRequestException, AlreadyExistsException {
		String channelName = channelCreationRequest.getName();
		createChannelValidator.validate(channelCreationRequest);

		ChannelConfiguration channelConfiguration = channelDao.createChannel(channelName);
		URI channelUri = linkBuilder.buildChannelUri(channelConfiguration);
		return Response.created(channelUri).entity(
				linked(channelConfiguration)
						.withLink("self", channelUri)
						.withLink("latest", linkBuilder.buildLatestUri(channelName))
						.withLink("ws", linkBuilder.buildWsLinkFor(channelName))
						.build())
					   .build();
	}
}
