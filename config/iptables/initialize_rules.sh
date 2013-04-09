#!/bin/sh



######################################################
# these lines are the requires minimum configuration #
# for the ip-tables-client to run correctly          #
######################################################

# flush all iptables-rules
#iptables --flush

# set default input policy
#iptables -P INPUT DROP

# set default forward policy
#iptables -P FORWARD DROP

# insert log rules for input and forward table
iptables -I INPUT -j ULOG
iptables -I FORWARD -j ULOG

# Always accept loopback traffic
iptables -A INPUT -i lo -j ACCEPT

# allow comunication with map-server
# a.t.m this rule will be automatically created by the ifmapclient!
# iptables -A INPUT -s 10.10.253.2 -j ACCEPT



##########################################################
# put your custom rules (or script to be executed) below #
##########################################################

# example-script for gateway-configuration used in demonstrator
# taken from http://www.debian-administration.org/articles/23

# enable ip forwarding in ip-tables-machine
#echo 1 > /proc/sys/net/ipv4/ip_forward

# allow forwarding of established connections from intern (eth1) to extern (eth0)
#iptables -A FORWARD -i eth1 -o eth0 -j ACCEPT
#iptables -A FORWARD -i eth0 -o eth1 -m state --state ESTABLISHED,RELATED -j ACCEPT

# allow input comming from intern (eth1)
#iptables -A INPUT -i eth1 -j ACCEPT
#iptables -A INPUT -i eth1 -m state --state ESTABLISHED,RELATED -j ACCEPT

# Masquerade.
#iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE



##########################################################
# some usefull examples...                               #
##########################################################

# iptables -I FORWARD -s 10.10.253.24 -j ACCEPT 
# iptables -I FORWARD -s 10.10.253.93 -j ACCEPT
# iptables --flush && iptables -P INPUT ACCEPT && iptables -P FORWARD ACCEPT && iptables --list -v
