
This is a simple example app for demonstrating the PanicKit for
sending panic triggers to other apps.

https://dev.guardianproject.info/projects/panic/wiki

For an example of this interaction, check out this demo video:
https://www.youtube.com/watch?v=mS1gstS6YS8


Building with Android Studio
----------------------------

Just import this as a regular Android Studio gradle project, and it
should work as it is, and download the panic jar via gradle.


Building with Eclipse ADT
-------------------------

A skeleton project is included in this git repo, so you can import
this project into Eclipse:

1. Start by adding FakePanicButton to Eclipse by going to _File_ -> _New_ ->
_Project..._ -> _Android project from existing code_.

2. Open the FakePanicButton folder that was just cloned from git.

Now you should be ready to work with FakePanicButton!


Building with ant
-----------------

We use `ant` to make our official releases and automated test builds.  If you
are not familiar with Eclipse, then it is easier to start with the `ant`
build:

    export ANDROID_HOME=/path/to/android-sdk
    ./setup-ant
    ant clean debug

Then the installable APK will be in **bin/FakePanicButton-debug.apk**.

