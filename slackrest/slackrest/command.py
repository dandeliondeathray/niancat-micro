from parse import *
from collections import namedtuple

Request = namedtuple("Request", ["url", "body", "method"])

class CommandParser:
    def __init__(self, commands=None):
        self._commands = commands or []

    def parse(self, msg, channel_id, user_id, user_name, self_name, visibility):
        stripped_msg = msg.strip()
        for command in self._commands:
            command_has_visibility = not command.visibility == Visibility.Any
            visibility_matches_message = command.visibility == visibility
            if command_has_visibility and not visibility_matches_message:
                continue
            if self_name == user_name:
                continue

            match = parse(command.pattern, stripped_msg)
            if match:
                arguments = match.named
                arguments['user_id'] = user_id
                arguments['channel_id'] = channel_id
                arguments['user_name'] = user_name
                url = command.url_format.format(**arguments)
                if command.body:
                    request_body = command.body(**arguments)
                else:
                    request_body = None
                return Request(url, request_body, command.method)

class Visibility:
    Any = 0
    Public = 1
    Private = 2

    @classmethod
    def parse(cls, channel_id):
        if channel_id.startswith('C'):
            return Visibility.Public
        elif channel_id.startswith('D'):
            return Visibility.Private


class UnknownMethodException(RuntimeError):
    def __init__(self, *args, **kwargs):
        RuntimeError.__init__(self, *args, **kwargs)


class Method:
    GET = 0
    POST = 1
    PUT = 2
    UPDATE = 3
    DELETE = 4
    HEAD = 5
    PATCH = 6

    @classmethod
    def parse(cls, s):
        methods = {
            'GET': Method.GET,
            'PUT': Method.PUT,
            'POST': Method.POST,
            'DELETE': Method.DELETE,
            'UPDATE': Method.UPDATE,
            'HEAD': Method.HEAD,
            'PATCH': Method.PATCH
        }
        try:
            return methods[s]
        except KeyError:
            raise UnknownMethodException()

    @classmethod
    def serialize(cls, method):
        names = {
            Method.GET: 'GET',
            Method.PUT: 'PUT',
            Method.POST: 'POST',
            Method.DELETE: 'DELETE',
            Method.UPDATE: 'UPDATE',
            Method.HEAD: 'HEAD',
            Method.PATCH: 'PATCH'
        }
        try:
            return names[method]
        except KeyError:
            raise UnknownMethodException()