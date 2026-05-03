Railway Interlocking System
Overview

This project implements a basic railway interlocking system in Java. The goal is to manage train movements safely across different track sections while preventing conflicts such as collisions or unsafe movements.

Each train follows a fixed route from its entry section to its destination, and the system ensures that all movements are safe before they are executed.

Key Features
Only one train is allowed in each section at any time
Trains move one section at a time
All moves are checked before being applied
Invalid routes are rejected
Trains exit the system once they reach their destination
How the System Works
Train Movement

The system uses a two-step process when moving trains:

First, it checks which trains can move safely
Then, it moves all valid trains together

This avoids situations where one train moves and causes a conflict for another in the same cycle.

Route Handling

Each train follows a predefined route depending on its entry and destination.

Example:

1 → 4
1 → 8
3 → 4
4 → 3

If a route is not valid, the system will throw an error.

Safety Rules

The system prevents:

Two trains entering the same section
Two trains swapping sections at the same time
Moving into an occupied section

There are also a few extra checks for specific junction situations to avoid unsafe movements.

Exit Behaviour
When a train reaches its destination, it exits on the next move
After exiting, its section becomes free
getTrain() returns -1 for exited trains
Main Methods
addTrain()

Adds a new train with a valid route.

moveTrains()

Moves trains safely and returns how many moved.

getSection()

Returns the train in a section (or null if empty).

getTrain()

Returns the current section of a train, or -1 if it has exited.

How to Run

Compile:

javac *.java

Run tests:

java org.junit.runner.JUnitCore InterlockingImpl_Test
Notes
The implementation focuses on keeping the logic simple and stable
Only necessary safety checks are used to avoid over-complicating the system
The design matches the behaviour expected by the assignment tests
Author

Dhruvit Navin Desai
Adelaide University
A1986442