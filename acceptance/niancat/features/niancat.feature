Feature: Niancat as a service
  "Dagens Nia" (literally "Todays Nine") is a puzzle in the Swedish newspaper Svenska Dagbladet.
  Nine letters are provided, in a scrambled order, and the goal is to figure out which nine letter word it is.
  There may be one or more solutions.
  Niancat is a bot which aids us in checking if a word is a solution to the puzzle, or not. It does this by checking
  that the word matches the letters in the puzzle and that it's present in a dictionary.

  A REPLY is a response sent to the Slack channel where the command was received.
  A NOTIFICATION is a response sent to a pre-defined public channel where people see results.

  @wip
  Scenario: No puzzle is set
    Given that no puzzle is set
     When Christian gets the puzzle
     Then he gets a reply that the puzzle is not set

  Scenario: Get a puzzle
    Given the puzzle is set to DATORSPLE
     When Christian gets the puzzle
     Then he gets a reply containing "DAT ORS PLE"

  Scenario: Solve a puzzle
    Given the puzzle is set to DATORSPLE
     When Johan tests the solution DATORSPEL
     Then he gets a reply that it is correct
      And there is a notification that he solved the puzzle

  Scenario: Check an incorrect solution
    Given the puzzle is set to DATORSPLE
     When Viktor tests the solution DATORLESP
     Then he gets a reply that it is incorrect

  Scenario: Solutions are reported on next puzzle
    Given the puzzle is set to DATORSPLE
      And Johan tests the solution DATORSPEL
     When the puzzle is set to DATORSPLE
     Then Johan is listed as solving DATORSPEL in the notification channel

  Scenario: A notification is sent when a puzzle with multiple solutions is set
     When the puzzle is set to DATORSPLE
     Then there is a notification that there are two solutions

  Scenario: Setting an unsolution
    Given that Veronica has stored an unsolution "Unsolution 1"
     When she lists unsolutions
     Then the unsolutions contain "Unsolution 1"

  Scenario: Unsolutions are listed on next puzzle
    Given the puzzle is set to DATORSPLE
      And that Veronica has stored an unsolution "ABC DEF GHI"
     When the puzzle is set to VANTRIVSA
     Then the unsolution "ABC DEF GHI" is listed with Veronicas name

  @wip
  Scenario: Showing mismatches for potential solutions
    Given the puzzle is set to DATORSPLE
     When Andreas tests the solution DATORSPEXY
     Then he gets a reply that there are too many XY and too few L
