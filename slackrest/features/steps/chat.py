from behave import given, when, then
from slackrest.app import SlackrestApp
from slackrest.command import Visibility, Method
import json


class GiveMeAReply:
    pattern = '!givemeareply'
    url_format = '/reply'
    visibility = Visibility.Any
    body = None
    method = Method.GET


class GiveMeANotification:
    pattern = '!givemeanotification'
    url_format = '/notify'
    visibility = Visibility.Any
    body = None
    method = Method.GET


class MakeAPost:
    pattern = '!makeapost'
    url_format = '/makeapost'
    visibility = Visibility.Any
    method = Method.POST

    @classmethod
    def body(cls, **kwargs):
        return json.dumps({'param': 'value'})


commands = [GiveMeAReply, GiveMeANotification, MakeAPost]


@given(u'Slackrest is connected to Slack')
def step_impl(context):
    context.app = SlackrestApp(context.chat_url, commands, context.notification_channel_id)
    context.app.run_async()
    context.slack_events.await_event(event_type='login')


@when(u'I send "{message}" from channel "{channel_id}"')
def step_impl(context, message, channel_id):
    user_id = 'U123456'
    msg = {'type': 'message', 'text': message, 'channel': channel_id, 'user': user_id}
    context.slack_events.send_message(msg)


@then(u'I should get a message in channel "{channel_id}"')
def step_impl(context, channel_id):
    event = context.slack_events.await_event(event_type='message')
    assert event['message']['channel'] == channel_id


@then(u'I should get a message containing "{msg}"')
def step_impl(context, msg):
    event = context.slack_events.await_event(event_type='message')
    print("Got message containing '{}'".format(event['message']['text']))
    print("Got message containing '{}'".format(event['message']['text']))
    assert msg in event['message']['text']


@given(u'I set the notification channel to "{notification_channel_id}"')
def step_impl(context, notification_channel_id):
    context.notification_channel_id = notification_channel_id


@given(u'I map "!givemeareply" to /reply')
def step_impl(context):
    pass


@given(u'I map "!givemeanotification" to /notify')
def step_impl(context):
    pass


@given(u'the chat bot is at {url}')
def step_impl(context, url):
    context.chat_url = url