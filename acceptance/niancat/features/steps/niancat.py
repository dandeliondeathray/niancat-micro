# -*- coding: utf8 -*-

from behave import *
from hamcrest import *


@given(u'that no puzzle is set')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given that no puzzle is set')


@when(u'Christian gets the puzzle')
def step_impl(context):
    assert_that(context.christian.send_message("!nian", "konsulatet"), "Christian gets the puzzle")
    context.expect_reply_in = context.konsulatet_channel_id


@then(u'he gets a reply that the puzzle is not set')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then he gets a reply that the puzzle is not set')


@given(u'the puzzle is set to DATORSPLE')
def step_impl(context):
    assert_that(context.erik.send_message("!s\\uE4ttnian DATORSPLE", "konsulatet"), "Erik sets the puzzle to DATORSPLE")


@then(u'he gets a reply containing "DAT ORS PLE"')
def step_impl(context):
    m = context.happenings.await_message()
    assert_that(m['text'], contains('DAT ORS PLE'))
    assert_that(m['channel'], equal_to(context.expect_reply_in))


@when(u'Johan tests the solution DATORSPEL')
def step_impl(context):
    raise NotImplementedError(u'STEP: When Johan tests the solution DATORSPEL')


@then(u'he gets a reply that it is correct')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then he gets a reply that it is correct')


@then(u'there is a notification that he solved the puzzle')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then there is a notification that he solved the puzzle')


@when(u'Viktor tests the solution DATORLESP')
def step_impl(context):
    raise NotImplementedError(u'STEP: When Viktor tests the solution DATORLESP')


@then(u'he gets a reply that it is incorrect')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then he gets a reply that it is incorrect')


@given(u'Johan tests the solution DATORSPEL')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given Johan tests the solution DATORSPEL')


@when(u'the puzzle is set to DATORSPLE')
def step_impl(context):
    raise NotImplementedError(u'STEP: When the puzzle is set to DATORSPLE')


@then(u'Johan is listed as solving DATORSPEL in the notification channel')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then Johan is listed as solving DATORSPEL in the notification channel')


@then(u'there is a notification that there are two solutions')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then there is a notification that there are two solutions')


@given(u'that Veronica has stored an unsolution "Unsolution 1"')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given that Veronica has stored an unsolution "Unsolution 1"')


@when(u'she lists unsolutions')
def step_impl(context):
    raise NotImplementedError(u'STEP: When she lists unsolutions')


@then(u'the unsolutions contain "Unsolution 1"')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then the unsolutions contain "Unsolution 1"')


@given(u'that Veronica has stored an unsolution "ABC DEF GHI"')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given that Veronica has stored an unsolution "ABC DEF GHI"')


@when(u'the puzzle is set to VANTRIVSA')
def step_impl(context):
    raise NotImplementedError(u'STEP: When the puzzle is set to VANTRIVSA')


@then(u'the unsolution "ABC DEF GHI" is listed with Veronicas name')
def step_impl(context):
    raise NotImplementedError(u'STEP: Then the unsolution "ABC DEF GHI" is listed with Veronicas name')
