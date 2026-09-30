package in.infosys.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name="credentials")
public class Credential {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String title; private String username;
    @Column(nullable=false) private String password;
    private String website;
    @Column(length=4000) private String notes;
    @Column(length=80) private String category;
    @Column(length=50) private String credentialType;
    @Column(nullable=false) private boolean favorite=false;
    @ManyToOne @JoinColumn(name="user_id") private User user;
    private boolean deleted=false;
    @ManyToOne @JoinColumn(name="team_id") private Team team;
    public Credential(){}
    public Credential(Long id,String title,String username,String password,String website,String notes,User user,boolean deleted,Team team){this.id=id;this.title=title;this.username=username;this.password=password;this.website=website;this.notes=notes;this.user=user;this.deleted=deleted;this.team=team;}
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getWebsite(){return website;} public void setWebsite(String v){website=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getCredentialType(){return credentialType;} public void setCredentialType(String v){credentialType=v;}
    public boolean isFavorite(){return favorite;} public void setFavorite(boolean v){favorite=v;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public boolean isDeleted(){return deleted;} public void setDeleted(boolean v){deleted=v;} public Team getTeam(){return team;} public void setTeam(Team v){team=v;}
}
