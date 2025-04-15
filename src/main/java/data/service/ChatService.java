package data.service;

import data.dto.ChatGroupsDto;
import data.dto.ChatLogDto;
import data.dto.ChatRoomData;
import data.dto.UsersDto;
import data.mapper.ChatGroupsMapper;
import data.mapper.ChatLogMapper;
import data.mapper.UsersMapper;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatGroupsMapper chatGroupsMapper;

    @Autowired
    private ChatLogMapper chatLogMapper;

    @Autowired
    private UsersMapper usersMapper;

    // 사용자가 속한 그룹 목록과 마지막 메시지 조회
    public List<ChatGroupsDto> readAllGroupsWithLastMessages(Integer userId) {
    	List<ChatGroupsDto> groups = chatGroupsMapper.readAllGroupsWithLastMessages(Long.valueOf(userId));
        for (ChatGroupsDto group : groups) {
            // 각 채팅방의 사용자 수를 조회하여 memberCount 설정
            List<Long> groupUserIds = chatGroupsMapper.readGroupUserIds(Long.valueOf(group.getId()));
            group.setMemberCount(groupUserIds != null ? groupUserIds.size() : 0);
        }
        return groups;
    }

    // 특정 그룹 정보 조회
    public ChatGroupsDto readGroupById(Integer firstChatId) {
        return chatGroupsMapper.readGroupById(firstChatId);
    }

    // 그룹 채팅 메시지 조회
    public List<ChatLogDto> readGroupMessages(Integer firstChatId) {
        List<ChatLogDto> messages = chatLogMapper.readAllLogsByGroupId(Long.valueOf(firstChatId));
        for (ChatLogDto msg : messages) {
            UsersDto sender = usersMapper.readUserById(msg.getSenderId());
            if (sender != null) {
                msg.setSenderName(sender.getName());
            }
        }
        return messages;
    }

    // 1:1 채팅 메시지 조회
    public List<ChatLogDto> readPrivateMessages(Integer userId, Integer chatId) {
        Map<String, Integer> params = new HashMap<>();
        params.put("userId", userId);
        params.put("chatId", chatId);
        List<ChatLogDto> messages = chatLogMapper.readAllPrivateLogs(params);
        for (ChatLogDto msg : messages) {
            UsersDto sender = usersMapper.readUserById(msg.getSenderId());
            if (sender != null) {
                msg.setSenderName(sender.getName());
            }
        }
        return messages;
    }

    // 채팅 메시지 저장
    public void createChatMessage(ChatLogDto chatLogDto) {
        if (chatLogDto == null) {
            logger.error("ChatLogDto is null");
            throw new IllegalArgumentException("ChatLogDto cannot be null");
        }
        if (chatLogDto.getSenderId() == null) {
            logger.error("Sender ID is null in ChatLogDto: {}", chatLogDto);
            throw new IllegalArgumentException("Sender ID cannot be null");
        }
        if (chatLogDto.getMessage() == null || chatLogDto.getMessage().trim().isEmpty()) {
            logger.error("Message content is empty in ChatLogDto: {}", chatLogDto);
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (chatLogDto.getRoomId() == null) {
            logger.error("Room ID is null in ChatLogDto: {}", chatLogDto);
            throw new IllegalArgumentException("Room ID cannot be null");
        }
        logger.info("Saving ChatLogDto: {}", chatLogDto);
        chatLogMapper.createChatLog(chatLogDto);
        logger.info("ChatLogDto saved successfully");
    }

    // 사용자 정보 조회
    public UsersDto readUserById(Integer userId) {
        return usersMapper.readUserById(userId);
    }

    // 사용자가 속한 그룹의 사용자 목록 조회
    public List<Long> readGroupUserIds(Integer groupId) {
        return chatGroupsMapper.readGroupUserIds(Long.valueOf(groupId));
    }

    // 현재 사용자를 제외한 모든 사용자 목록 조회 (contacts용)
    public List<UsersDto> readAllUsersExceptCurrent(Integer userId) {
        List<UsersDto> allUsers = usersMapper.readAllActiveUsers();
        if (allUsers == null || userId == null) {
            return List.of();
        }
        return allUsers.stream()
                .filter(user -> user.getId() != userId)
                .collect(Collectors.toList());
    }
    
    public void createGroupUser(Integer userId, Integer groupId) {
        if (userId == null || groupId == null) {
            logger.error("User ID or Group ID is null: userId={}, groupId={}", userId, groupId);
            throw new IllegalArgumentException("User ID and Group ID cannot be null");
        }
        // 그룹 존재 여부 확인
        ChatGroupsDto group = chatGroupsMapper.readGroupById(groupId);
        if (group == null) {
            logger.error("Group not found: groupId={}", groupId);
            throw new IllegalArgumentException("Group not found");
        }
        // 사용자 존재 여부 확인
        UsersDto user = usersMapper.readUserById(userId);
        if (user == null) {
            logger.error("User not found: userId={}", userId);
            throw new IllegalArgumentException("User not found");
        }
        // 이미 그룹에 있는지 확인
        List<Long> groupUserIds = chatGroupsMapper.readGroupUserIds(Long.valueOf(groupId));
        if (groupUserIds.contains(Long.valueOf(userId))) {
            logger.warn("User already in group: userId={}, groupId={}", userId, groupId);
            return; // 이미 있으면 추가하지 않음
        }
        // 그룹에 사용자 추가
        chatGroupsMapper.createGroupUser(Long.valueOf(userId), Long.valueOf(groupId));
        logger.info("User invited to group: userId={}, groupId={}", userId, groupId);
    }
    
    @Transactional
    public Integer createGroup(String groupName, Long createdBy, List<Long> invitedUserIds) {
        // 1. 그룹 생성
        ChatGroupsDto group = new ChatGroupsDto();
        group.setName(groupName);
        group.setCreatedBy(createdBy);
        logger.info("Inserting group with name: {}", groupName);
        chatGroupsMapper.createGroup(group); // 그룹 삽입

        // 2. 삽입 후 groupId 확인
        Integer groupId = group.getId();
        logger.info("Inserted group with ID: {}", groupId);
        if (groupId == null) {
            throw new IllegalStateException("그룹 생성 후 groupId가 설정되지 않았습니다.");
        }

        // 3. 그룹에 사용자 추가
        logger.info("Adding user {} to group {}", createdBy, groupId);
        createGroupUser(createdBy.intValue(), groupId); // 그룹 생성자 추가
        if (invitedUserIds != null) {
            for (Long invitedUserId : invitedUserIds) {
                logger.info("Adding invited user {} to group {}", invitedUserId, groupId);
                createGroupUser(invitedUserId.intValue(), groupId); // 초대된 사용자 추가
            }
        }
        return groupId; // 생성된 groupId 반환
    }
    
    // 개인 채팅 생성
    public Integer createPrivateChat(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null) {
            logger.error("User ID or Target User ID is null: userId={}, targetUserId={}", userId, targetUserId);
            throw new IllegalArgumentException("User ID and Target User ID cannot be null");
        }

        ChatGroupsDto existingChat = chatGroupsMapper.readPrivateChatBetweenUsers(userId, targetUserId);
        if (existingChat != null) {
            logger.info("Private chat already exists between userId={} and targetUserId={}: chatId={}", userId, targetUserId, existingChat.getId());
            return existingChat.getId();
        }

        ChatGroupsDto privateChat = new ChatGroupsDto();
        privateChat.setName("");
        privateChat.setCreatedBy(userId);
        privateChat.setRoomType("PRIVATE");
        chatGroupsMapper.createGroup(privateChat);

        Long chatId = Long.valueOf(privateChat.getId());
        logger.info("Created private chat with ID: {}", chatId);

        chatGroupsMapper.createGroupUser(userId, chatId);
        chatGroupsMapper.createGroupUser(targetUserId, chatId);

        return privateChat.getId();
    }
    
    //파일 업로드 및 다운로드
    public void createFileMessage(ChatLogDto chatLogDto) {
        if (chatLogDto == null) {
            logger.error("ChatLogDto가 null입니다.");
            throw new IllegalArgumentException("ChatLogDto는 null일 수 없습니다.");
        }
        // groupId가 null이면 roomId로 설정
        if (chatLogDto.getGroupId() == null) {
            chatLogDto.setGroupId(chatLogDto.getRoomId());
            logger.info("groupId가 null이므로 roomId로 설정: groupId={}", chatLogDto.getGroupId());
        }
        logger.info("파일 메시지 저장: {}", chatLogDto);
        try {
            chatLogMapper.createChatLog(chatLogDto);
            logger.info("파일 메시지 저장 성공: {}", chatLogDto.getFile());
        } catch (Exception e) {
            logger.error("파일 메시지 저장 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 메시지 저장에 실패했습니다.", e);
        }
    }
    public ChatRoomData getPrivateChatById(Integer chatId, Integer userId) {
        ChatGroupsDto chat = chatGroupsMapper.readGroupById(chatId);
        if (chat == null || !chat.getRoomType().equals("PRIVATE")) {
            return null;
        }

        List<Long> userIds = chatGroupsMapper.readGroupUserIds(Long.valueOf(chatId));
        Long targetUserId = userIds.stream()
                .filter(id -> !id.equals(Long.valueOf(userId)))
                .findFirst()
                .orElse(null);

        if (targetUserId == null) {
            return null;
        }

        ChatRoomData chatRoomData = new ChatRoomData();
        chatRoomData.setRoomId(chatId);
        chatRoomData.setTargetUserId(targetUserId.intValue());
        return chatRoomData;
    }
    
    public void openChat(Long chatId, HttpSession session) {
        // 1. 세션에서 기존 openChats 리스트 가져오기 (없으면 새로 만듦)
        List<Long> openChats = (List<Long>) session.getAttribute("openChats");
        if (openChats == null) {
            openChats = new ArrayList<>();
        }

        // 2. 해당 채팅방이 리스트에 없으면 추가
        if (!openChats.contains(chatId)) {
            openChats.add(chatId);
        }

        // 3. 다시 세션에 저장
        session.setAttribute("openChats", openChats);

        // 4. 첫 번째 채팅방 선택용 값도 세션에 저장 (처음 열었을 때만)
        if (session.getAttribute("firstChatId") == null) {
            session.setAttribute("firstChatId", chatId);
        }

        // (선택 사항) 채팅방 이름, 채팅 내역 등도 세션에 저장 가능
    }    
}