package in.infosys.backend.dto;
import in.infosys.backend.entity.Credential;
public class CredentialResponseDto {
 private Long id; private String title,username,password,website,notes; private Boolean deleted; private String category,credentialType; private boolean favorite;
 public CredentialResponseDto(){}
 public static CredentialResponseDto fromEntity(Credential c){CredentialResponseDto d=new CredentialResponseDto();d.id=c.getId();d.title=c.getTitle();d.username=c.getUsername();d.password=c.getPassword();d.website=c.getWebsite();d.notes=c.getNotes();d.deleted=c.isDeleted();d.category=c.getCategory();d.credentialType=c.getCredentialType();d.favorite=c.isFavorite();return d;}
 public Long getId(){return id;} public void setId(Long v){id=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getUsername(){return username;} public void setUsername(String v){username=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;} public String getWebsite(){return website;} public void setWebsite(String v){website=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public Boolean getDeleted(){return deleted;} public void setDeleted(Boolean v){deleted=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getCredentialType(){return credentialType;} public void setCredentialType(String v){credentialType=v;} public boolean isFavorite(){return favorite;} public void setFavorite(boolean v){favorite=v;}
}
