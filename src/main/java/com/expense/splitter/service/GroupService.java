package com.expense.splitter.service;

import com.expense.splitter.entity.Group;
import com.expense.splitter.entity.GroupMember;
import com.expense.splitter.entity.User;
import com.expense.splitter.repository.GroupMemberRepository;
import com.expense.splitter.repository.GroupRepository;
import com.expense.splitter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(Group group){
        return groupRepository.save(group);
    }

    public void addUserToGroup(Long groupId, Long userId){

        Group group = groupRepository.findById(groupId).orElseThrow();

        User user = userRepository.findById(userId).orElseThrow();

        GroupMember member = new GroupMember();

        member.setGroup(group);
        member.setUser(user);

        groupMemberRepository.save(member);
    }

    public List<GroupMember> getGroupMembers(Long groupId){
        return groupMemberRepository.findByGroupId(groupId);
    }
}
