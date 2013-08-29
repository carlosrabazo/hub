package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateChannelValidatorTest {
	@Test
	public void testAllGood() throws InvalidRequestException, AlreadyExistsException {
		//GIVEN
		ChannelDao dao = mock(ChannelDao.class);
		String channelName = "a_channel";

        ChannelCreationRequest request = ChannelCreationRequest.builder().withName(channelName).build();

		//WHEN
		CreateChannelValidator testClass = new CreateChannelValidator(dao);
		when(dao.channelExists(channelName)).thenReturn(false);
		testClass.validate(request);

		//THEN
		//success
	}

	@Test(expected = InvalidRequestException.class)
	public void testChannelNameNull() throws InvalidRequestException, AlreadyExistsException {
		//GIVEN
		ChannelDao dao = mock(ChannelDao.class);
        ChannelCreationRequest request = ChannelCreationRequest.builder().withName(null).build();

		//WHEN
		CreateChannelValidator testClass = new CreateChannelValidator(dao);
		when(dao.channelExists(any(String.class))).thenReturn(false);
		testClass.validate(request);
	}

	@Test(expected = InvalidRequestException.class)
	public void testChannelNameEmpty() throws InvalidRequestException, AlreadyExistsException {
		//GIVEN
		ChannelDao dao = mock(ChannelDao.class);
        ChannelCreationRequest request = ChannelCreationRequest.builder().withName("").build();

		//WHEN
		CreateChannelValidator testClass = new CreateChannelValidator(dao);
		when(dao.channelExists(any(String.class))).thenReturn(false);
		testClass.validate(request);
	}

	@Test(expected = InvalidRequestException.class)
	public void testChannelNameBlank() throws InvalidRequestException, AlreadyExistsException {
		//GIVEN
		ChannelDao dao = mock(ChannelDao.class);
        ChannelCreationRequest request = ChannelCreationRequest.builder().withName("  ").build();

		//WHEN
		CreateChannelValidator testClass = new CreateChannelValidator(dao);
		when(dao.channelExists(any(String.class))).thenReturn(false);
		testClass.validate(request);
	}

	@Test(expected = AlreadyExistsException.class)
	public void testChannelExists() throws InvalidRequestException, AlreadyExistsException {
		//GIVEN
		ChannelDao dao = mock(ChannelDao.class);
		String channelName = "achannel";
        ChannelCreationRequest request = ChannelCreationRequest.builder().withName(channelName).build();

        //WHEN
		//THEN
		CreateChannelValidator testClass = new CreateChannelValidator(dao);
		when(dao.channelExists(channelName)).thenReturn(true);
		testClass.validate(request);
	}

	@Test(expected = InvalidRequestException.class)
	public void testInvalidSpaceCharacter() throws Exception {
		//GIVEN
		String channelName = "my chan";
		ChannelDao dao = mock(ChannelDao.class);
        ChannelCreationRequest request = ChannelCreationRequest.builder().withName(channelName).build();

		CreateChannelValidator testClass = new CreateChannelValidator(dao);

		//WHEN
		//THEN
		testClass.validate(request);
	}

	@Test(expected = InvalidRequestException.class)
	public void testInvalidCharacter() throws Exception {
		//GIVEN
		String channelName = "my#chan";
		ChannelDao dao = mock(ChannelDao.class);
        ChannelCreationRequest request = ChannelCreationRequest.builder().withName(channelName).build();

		CreateChannelValidator testClass = new CreateChannelValidator(dao);

		//WHEN
		//THEN
		testClass.validate(request);
	}
}
