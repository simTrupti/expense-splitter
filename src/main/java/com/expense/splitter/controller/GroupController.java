package com.expense.splitter.controller;

import com.expense.splitter.service.GroupService;

public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }
}
