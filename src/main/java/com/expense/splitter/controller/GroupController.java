package com.expense.splitter.controller;

import com.expense.splitter.entity.Group;
import com.expense.splitter.entity.GroupMember;
import com.expense.splitter.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Groups",
        description = "Create and manage expense-sharing groups"
)
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @Operation(summary = "Create a group")
    @PostMapping
    public Group createGroup(@RequestBody Group group){
        return groupService.createGroup(group);
    }

    @Operation(summary = "add user to group")
    @PostMapping("/{groupId}/users/{userId}")
    public String addUserToGroup(@PathVariable Long groupId, @PathVariable Long userId){

        groupService.addUserToGroup(groupId,userId);

        return "User added to group";
    }

    @Operation(summary = "Get group members")
    @GetMapping("/{groupId}/members")
    public List<GroupMember> getMembers(@PathVariable Long groupId){
        return groupService.getGroupMembers(groupId);
    }





}