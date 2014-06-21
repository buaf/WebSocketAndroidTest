#!/bin/bash

ant clean && ant release && ./andinstall.sh build com.example com.example.MainActivity
