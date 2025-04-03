import os
#from tornado.ioloop import IOLoop, PeriodicCallback
#from tornado import gen
from tornado.httpclient import HTTPClient, HTTPRequest
import json
from slackrest.command import Method, CommandParser, Visibility
#import sys
from collections import namedtuple
from slack_bolt import App
from slack_bolt.adapter.socket_mode import SocketModeHandler

class InvalidResponseType(Exception):
    def __init__(self):
        Exception.__init__(self)

IncomingMessage = namedtuple("IncomingMessage", ["message", "channel_id", "user_id", "user_name"])

#class SlackException(RuntimeError):
#    def __init__(self, *args, **kwargs):
#        RuntimeError.__init__(*args, **kwargs)

def read_token(name):
    try:
        token = os.environ[name].strip()
    except KeyError:
        print(f"You must specify a Slack API token in the '{name}' environment variable")
        return
    return token

def read_bot_token():
    return read_token("SLACK_BOT_TOKEN")

def read_app_token():
    return read_token("SLACK_APP_TOKEN")

#def handle_callback_exception(callback):
#    (ex_type, value, traceback) = sys.exc_info()
#    print("EXCEPTION {}: {}", ex_type, value)
#    print("Traceback: {}", traceback)
#    IOLoop.current().stop()
#
#def enqueue_response(message, channel_id):
#    print("Replying to Slack with message {} and channel id {}".format(message, channel_id))
#    #IOLoop.current().add_callback(self.sc.rtm_send_message, channel_id, message)
#
class RouteContext:
    def __init__(self, incoming_message, notification_channel_id):
        self.incoming_message = incoming_message
        self.notification_channel_id = notification_channel_id

    def response_channel(self, response):
        response_type = response['response_type']
        if response_type == 'reply':
            response_channel = self.incoming_message.channel_id
        elif response_type == 'notification':
            response_channel = self.notification_channel_id
        else:
            raise InvalidResponseType()
        return response_channel

#notification_channel_id = None
users = {}
#command_parser = None
self_name = "" # FIXME
self_id = None
base_url = None

def get_response(base_url, request, route_context):
    final_url = base_url + request.url
    client = HTTPClient()
    print(f"Request URL: {final_url}")
    http_request = HTTPRequest(final_url, method=Method.serialize(request.method), body=request.body)
    print(f"HTTP request: {http_request!s}")
    try:
        http_response = client.fetch(http_request)
        responses = json.loads(http_response.buffer.getvalue().decode("utf-8"))
        print("Responses: {}".format(responses))
        return [(route_context.response_channel(r), r) for r in responses]
    except Exception as e:
        print(f"HTTP Response Error: {e}")

#@slack.RTMClient.run_on(event="open")
#def handle_open(**payload):
#    global self_id
#    print(f"Open: {payload!s}")
#    self_id = payload["data"]["self"]["id"]
#
#@slack.RTMClient.run_on(event="message")
#def handle_message(**payload):
#    #print(f"Payload: {payload!s}")
#    message = payload["data"]
#    subtype = message.get('subtype', None)
#    if subtype is None or subtype == 'bot_message':
#        web_client = payload["web_client"]
#        channel_id = message['channel']
#        if subtype == 'bot_message':
#            user_id = message['bot_id']
#            user_name = message['username']
#        else:
#            user_id = message['user']
#            if user_id == self_id:
#                print("Ignoring message from myself")
#                return
#            try:
#                user_name = users[user_id]
#            except KeyError:
#                user_name = '<unknown user name>'
#        incoming_message = IncomingMessage(message, channel_id, user_id, user_name)
#        #print(f"Message: {incoming_message!s}")
#        route_context = RouteContext(web_client, incoming_message, notification_channel_id)
#        visibility = Visibility.parse(channel_id)
#        # return self.handle_command(incoming_message, route_context, visibility)
#        #print(f"Visibility: {visibility!s}  Route context: {route_context!s}  Incoming message: {incoming_message!s}")
#        request = command_parser.parse(incoming_message.message["text"],
#                                       incoming_message.channel_id,
#                                       incoming_message.user_id,
#                                       incoming_message.user_name,
#                                       self_name,
#                                       visibility)
#        if request:
#            print(f"Request {request!s}")
#            responses = get_response(request, route_context)
#            for response_channel, response in responses:
#                if response["message"]:
#                    web_client.chat_postMessage(
#                        channel=response_channel,
#                        as_user=True,
#                        text=response["message"]
#                    )
#        else:
#            print("Ignoring message")
#    else:
#        print(f"Subtype message: {message!s}")

#def run_forever(url, commands, not_channel_id):
#    global notification_channel_id
#    global users
#    global command_parser
#    global base_url
#    token = read_token()
#    if token is None:
#        return
#    web_client = slack.WebClient(token=token)
#    users_list = web_client.users_list()
#    #print(f"Users list: {users_list!s}")
#    is_users_list_ok = users_list['ok']
#    if not is_users_list_ok:
#        print(f"Users list not OK: {users_list!s}")
#        return
#    users = {
#        member['id']: member['name']
#        for member in users_list['members']
#    }
#    command_parser = CommandParser(commands)
#    client = slack.RTMClient(token=token)
#    notification_channel_id = not_channel_id
#    base_url = url
#    print(f"Base URL: {base_url}")
#    if client is None:
#        return
#    client.start()

def run_forever(base_url, commands, notification_channel_id):
    bot_token = read_bot_token()
    app_token = read_app_token()
    app = App(token=bot_token)

    # @app.message("hello")
    # def message_hello(message, say):
    #     print(f"Message: {message}")
    #     say(f"Hey there <@{message['user']}>!")

    command_parser = CommandParser(commands)

    @app.message()
    def handle_message(message, client, say):
        #print(f"Payload: {payload!s}")
        subtype = message.get('subtype', None)
        if subtype is None or subtype == 'bot_message':
            channel_id = message['channel']
            if subtype == 'bot_message':
                user_id = message['bot_id']
                user_name = message['username']
            else:
                user_id = message['user']
                if user_id == self_id:
                    print("Ignoring message from myself")
                    return
                try:
                    user_name = users[user_id]
                except KeyError:
                    user_name = '<unknown user name>'
            incoming_message = IncomingMessage(message, channel_id, user_id, user_name)
            print(f"Message: {incoming_message!s}")
            #say(f"I received '{incoming_message}'")
            route_context = RouteContext(incoming_message, notification_channel_id)
            visibility = Visibility.parse(channel_id)
            # return self.handle_command(incoming_message, route_context, visibility)
            print(f"Visibility: {visibility!s}  Route context: {route_context!s}  Incoming message: {incoming_message!s}")
            request = command_parser.parse(incoming_message.message["text"],
                                           incoming_message.channel_id,
                                           incoming_message.user_id,
                                           incoming_message.user_name,
                                           self_name,
                                           visibility)
            if request:
                print(f"Request {request!s}")
                responses = get_response(base_url, request, route_context)
                for response_channel, response in responses:
                    if response["message"]:
                        client.chat_postMessage(
                            channel=response_channel,
                            as_user=True,
                            text=response["message"]
                        )
            else:
                print("Ignoring message")
        else:
            print(f"Subtype message: {message!s}")

    socket_mode_handler = SocketModeHandler(app, app_token)
    socket_mode_handler.start()