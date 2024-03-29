osgi-stresser
=============

OSGI stress test utility, executes framework operations semi-randomly.

Usage
=====

Install this bundle at a low start level, so that it doesn't stop itself
when changing start levels.

Use telnet to connect to localhost:1234 to use the command interface. Port number can
be set via a system property, see source code.

A simplistic command interpreter is provided, see example commands below.

The stress testing tasks log what they are doing, at the INFO level.

## Commands

### bu o
Starts the "bundles" task in one shot mode: one cycle is executed, where
a semi-random set of bundles is stopped and restarted. Framework and other
critical bundles are not touched, see source code for details.

### bu r
Run the "bundles" task continuously.

### bu p
Pause the "bundles" task at the end of the next cycle.

### sl o 12 5 42 9 30
Runs one cycle of the "start levels" task, setting start levels from 12 to 30
in sequence as specified.

### sl o
Runs one cycle of the "start levels" task, reusing the same start levels sequence
as last time.

## sl r and sl p
Run and pause the start levels task. 

## up r -500
Run the "bundle upgrade" task continuously, waiting up to 500 msec between 
cycles.

## up o and up p
Run one cycle and pause the bundle ugprade task.

## rp r -500
Run the "refresh packages" task continuously, waiting up to 500 msec between 
cycles.

## rp o and rp p
Run one cycle and pause the refresh packages task.
