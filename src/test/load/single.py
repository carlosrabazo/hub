# locust.py

import json
import logging
import random
import socket
import string
import threading
import time
from datetime import datetime, timedelta
from flask import request, jsonify
from locust import HttpLocust, TaskSet, task, events, web

# This test uses the http://locust.io/ framework.
#
# It performs a combination of verification and load testing.
# Each channel (aka user) will perform all of the methods defined by @task(N)
# where N is the relative weighting for frequency.
#
# The tests in 'single.py' focus on creating high volume and verification of correct behavior of
# group callbacks, and that items are always increasing.
#
#
# Usage:
#   locust -f single.py -H http://hub
# or in the background with:
#   nohup locust -f single.py -H http://hub &
#
# After starting the process, go to http://locust:8089/

logger = logging.getLogger('hub-locust')
logger.setLevel(logging.INFO)
fh = logging.FileHandler('./locust.log')
fh.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
fh.setFormatter(formatter)
logger.addHandler(fh)

groupCallbacks = {}
groupConfig = {}


class WebsiteTasks(TaskSet):
    channelNum = 0

    def on_start(self):
        WebsiteTasks.channelNum += 1
        self.number = WebsiteTasks.channelNum
        self.payload = self.payload_generator()
        logger.info("payload size " + str(self.payload.__sizeof__()))
        self.channel = "single_test_" + str(self.number)
        self.count = 0
        payload = {"name": self.channel,
                   "ttlDays": "3",
                   "tags": ["load", "test", "DDT"],
                   "owner": "DDT",
                   "storage": "BATCH"}
        self.client.put("/channel/" + self.channel,
                        data=json.dumps(payload),
                        headers={"Content-Type": "application/json"},
                        name="channel")
        self.start_group_callback()
        time.sleep(5)

    def start_group_callback(self):
        # First User - create channel - posts to channel, parallel group callback on channel
        # Second User - create channel - posts to channel, parallel group callback on channel
        # Third User - create channel - posts to channel, minute group callback on channel
        group_channel = self.channel
        parallel = 10
        batch = "SINGLE"
        if self.number == 3:
            batch = "MINUTE"
            parallel = 1
        group_name = "/group/locust_" + group_channel
        self.client.delete(group_name, name="group")
        logger.info("group channel " + group_channel + " parallel:" + str(parallel))
        groupCallbacks[self.channel] = {
            "data": [],
            "lock": threading.Lock(),
            "parallel": parallel,
            "batch": batch
        }
        group = {
            "callbackUrl": "http://" + groupConfig['ip'] + ":8089/callback/" + self.channel,
            "channelUrl": groupConfig['host'] + "/channel/" + group_channel,
            "parallelCalls": parallel,
            "batch": batch
        }
        self.client.put(group_name,
                        data=json.dumps(group),
                        headers={"Content-Type": "application/json"},
                        name="group")

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self.client.base_url + "/channel/" + self.channel, 'GET')
        return json.loads(c)

    def write(self):
        payload = {"name": self.payload, "count": self.count}
        with self.client.post("/channel/" + self.channel, data=json.dumps(payload),
                              headers={"Content-Type": "application/json"}, catch_response=True,
                              name="post_payload") as postResponse:
            if postResponse.status_code != 201:
                postResponse.failure("Got wrong response on post: " + str(postResponse.status_code))

        links = postResponse.json()
        self.count += 1
        href = links['_links']['self']['href']
        self.append_href(href, groupCallbacks)
        return href

    def append_href(self, href, obj):
        try:
            obj[self.channel]["lock"].acquire()
            obj[self.channel]["data"].append(href)
            logger.debug('wrote %s', href)
        finally:
            obj[self.channel]["lock"].release()

    def read(self, uri):
        with self.client.get(uri, catch_response=True, name="get_payload") as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on get: " + str(postResponse.status_code) + " " + uri)

    @task(10000)
    def write_read(self):
        self.read(self.write())

    @task(10)
    def hour_query(self):
        self.client.get(self.time_path("hour"), name="time_hour")

    @task(10)
    def minute_query(self):
        self.client.get(self.time_path("minute"), name="time_minute")

    @task(1)
    def second_query(self):
        results = self.client.get(self.time_path("second"), name="time_second").json()

    def time_path(self, unit="second"):
        return "/channel/" + self.channel + "/time/" + unit + "?stable=false"

    @task(10)
    def next_batch_query(self):
        self.nextQueries("LONG_TERM_BATCH")

    def nextQueries(self, location):
        utcnow = datetime.utcnow()
        self.doNext(location, utcnow + timedelta(minutes=-1))
        self.doNext(location, utcnow + timedelta(hours=-1))
        self.doNext(location, utcnow + timedelta(days=-1))

    def doNext(self, location, time):
        path = "/channel/" + self.channel + time.strftime("/%Y/%m/%d/%H/%M/%S/000") + "/A/next/10?location=" + location
        with self.client.get(path, catch_response=True, name="next_" + location) as postResponse:
            if postResponse.status_code != 200:
                postResponse.failure("Got wrong response on next: " + str(postResponse.status_code))


    def payload_generator(self, chars=string.ascii_uppercase + string.digits):
        size = 3 * 1024
        return ''.join(random.choice(chars) for x in range(size))

    def verify_callback(self, obj, name="group"):
        obj[self.channel]["lock"].acquire()
        items = len(obj[self.channel]["data"])
        max = 2000
        if obj[self.channel]["batch"] == "MINUTE":
            max = 20000
        if items > max:
            events.request_failure.fire(request_type=name, name="length", response_time=1,
                                        exception=-1)
            logger.info(name + " too many items in " + self.channel + " " + str(items))
        obj[self.channel]["lock"].release()

    @task(10)
    def verify_callback_length(self):
        self.verify_callback(groupCallbacks, "group")

    @staticmethod
    def verify(channel, incoming_uri):
        if incoming_uri in groupCallbacks[channel]["data"]:
            (groupCallbacks[channel]["data"]).remove(incoming_uri)
            events.request_success.fire(request_type="group", name="verify", response_time=1,
                                        response_length=1)
        else:
            logger.info("missing parallel item " + str(incoming_uri))
            events.request_failure.fire(request_type="group", name="verify", response_time=1,
                                        exception=-1)

    @web.app.route("/callback/<channel>", methods=['GET', 'POST'])
    def callback(channel):
        if request.method == 'POST':
            incoming_json = request.get_json()
            for incoming_uri in incoming_json['uris']:
                if "_replicated" in incoming_uri:
                    incoming_uri = incoming_uri.replace("_replicated", "")
                if channel not in groupCallbacks:
                    logger.info("incoming uri before locust tests started " + str(incoming_uri))
                    return "ok"
                try:
                    groupCallbacks[channel]["lock"].acquire()
                    WebsiteTasks.verify(channel, incoming_uri)
                finally:
                    groupCallbacks[channel]["lock"].release()
            return "ok"
        else:
            return jsonify(items=groupCallbacks[channel]["data"])


class WebsiteUser(HttpLocust):
    task_set = WebsiteTasks
    min_wait = 1
    max_wait = 3

    def __init__(self):
        super(WebsiteUser, self).__init__()
        groupConfig['host'] = self.host
        groupConfig['ip'] = socket.gethostbyname(socket.getfqdn())
        logger.info('groupConfig %s', groupConfig)
        print groupConfig