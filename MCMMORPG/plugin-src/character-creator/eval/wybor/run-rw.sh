#!/usr/bin/env bash
# Jak run.sh, ale z zapisywalnym /clone-rw (reload.js zmienia config klonu).
E=/home/dev/.claude/jobs/2f21747c/tmp/wybor/eval; K=/home/dev/.claude/jobs/2f21747c/tmp/wybor/data
docker run --rm --network cc-nap-wybor-net -u 1000:1000 -e MC_HOST=cc-nap-wybor -e CLONE_LOG=/clone/logs/latest.log \
  -v $E:/e -v /home/dev/.claude/jobs/0c19ac97/tmp/eval/node_modules:/e/node_modules:ro \
  -v $K/logs:/clone/logs:ro -v $K:/clone-data:ro -v $K:/clone-rw -w /e node:20 node "$@"
