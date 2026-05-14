package com.expense.splitter.controller;

import com.expense.splitter.entity.Group;
import com.expense.splitter.entity.GroupMember;
import com.expense.splitter.service.GroupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public Group createGroup(@RequestBody Group group){
        return groupService.createGroup(group);
    }

    @PostMapping("/{groupId}/users/{userId}")
    public String addUserToGroup(@PathVariable Long groupId, @PathVariable Long userId){

        groupService.addUserToGroup(groupId,userId);

        return "User added to group";
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMember> getMembers(@PathVariable Long groupId){
        return groupService.getGroupMembers(groupId);
    }





}