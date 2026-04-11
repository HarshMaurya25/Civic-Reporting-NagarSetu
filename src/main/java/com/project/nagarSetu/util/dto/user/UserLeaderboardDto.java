package com.project.nagarSetu.util.dto.user;

public class UserLeaderboardDto {
    private String fullName;
    private Long score;
    public UserLeaderboardDto(String fullName, Long score) {
        this.fullName = fullName;
        this.score = score;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getScore() {  // Change from int to Long
        return score;
    }

    public void setScore(Long score) {  // Change from int to Long
        this.score = score;
    }
}
