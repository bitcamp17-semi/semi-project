package data.dto;

import lombok.Data;

import java.sql.Timestamp;

import org.apache.ibatis.type.Alias;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Alias("AlarmDto")
public class AlarmDto {
    public enum AlarmType {
        MESSAGE, SCHEDULE, BOARD, APPROVAL, SYSTEM
    }
    private int id;
    private AlarmType type; //타입
    private int isRead; //조회여부 확인용 boolean이었다가 오류나서 수정함
    private int userId; //알람받는 userId
    private int causedBy; //발생 id
    private String causedName;//발생시킨 이름
    private String content; //알람 내용
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private Timestamp createdAt;
}
