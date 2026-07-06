package member;

import java.time.LocalDateTime;

public class Member {
    private Long id;
    private String loginId;
    private String name;
    private String password;
    private LocalDateTime createAt;

    public Member(Long id, String loginId, String name, String password, LocalDateTime createAt) {
        this.id = id;
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.createAt = createAt;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }
}


