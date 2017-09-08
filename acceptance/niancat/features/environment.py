import time
from pytaut.happening import Happenings
from pytaut.control import Control


def before_feature(self, feature):
    # Sleep to allow the niancat service to start up
    time.sleep(5)


def before_scenario(context, scenario):
    context.happenings = Happenings()
    context.happenings.connect()
    context.control = Control()
    context.team = context.control.create_team("konsulatet")
    context.konsulatet_channel_id = context.team.add_channel("konsulatet")
    context.general_channel_id = context.team.add_channel("general")
    context.christian = context.team.add_user("Christian")
    context.erik = context.team.add_user("Erik")
    context.niancat = context.team.add_bot("niancat", "niancattoken", channels=["general", "konsulatet"])


def after_scenario(context, scenario):
    print("Closing happenings websocket")
    context.happenings.close()
