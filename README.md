# Overview
This repository provides a small test program that reproduces FlowRuleEvent reordering
reported in [ONOS-5093](https://jira.onosproject.org/browse/ONOS-5093).

# How to build
Just enter the following command under the project directory.

```bash
mvn clean install
```

# How to use
1. Start ONOS on your machine with
```bash
ok clean
```

2. Start Mininet
```bash
sudo mn --topo linear,10 --controller=remote,ip=<IP address of ONOS>
```

3. Run the application
There are multiple ways to deploy and run an ONOS application
([more info](https://wiki.onosproject.org/display/ONOS/Creating+and+deploying+an+ONOS+application)).
The recommended way here is using ONOS Web UI. Open "Application" in the menu appearing when
clicking the upper left corner button. Click the "+" button to install a new application.
The application file is `target/reorder-test-1.0-SNAPSHOT.oar`.

4. Check the log
You can check the log on your machine with
```bash
log:tail
```
or
```bash
log:display
```
You could see warning-level log outputs. The results may differ every execution because
multi-threading causes the issue.

# Notes
- Reorders of FlowRuleEvent can be observed when using ONOS whose commit hash is
914db1cf9fba4958c3d6963f5996c7b9c7b338e9