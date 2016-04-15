Welcome to a java clone of the popular Facebook and oft-cloned game - Chain Reaction. Here known as ChainRxn ripped directly off from Mobile1up.com's version (attribution to Aaron Ardiri).

Running
-------

Option 1:
Prior to building ensure you have a repository directory to nominate or use an existing repos you might have thus:

```mvn -Dlocal.repos.dir=<MY_LOCAL_REPOS_LOCATION> package```

(**Note: Pls specify absolute path here suitable for file:/// postfixing e.g. file:///c:/somepath).

```cd target```
```java -jar chainrxn-1.0-SNAPSHOT.jar```

Option 2 (simpler): 

```mvn clean package exec:exec```

Known Bugs
----------

- At completion of levels cannot return to start screen(!) - could be incomplete feature

Backlog
-------

Evernote note: http://goo.gl/U9rv9 - Feel free to have a crack!

--
`Author: Mark Hesketh`
`Date: 16-10-2015`
[![Build Status](https://travis-ci.org/mesketh/chainrxn.svg?branch=master)](https://travis-ci.org/mesketh/chainrxn)
