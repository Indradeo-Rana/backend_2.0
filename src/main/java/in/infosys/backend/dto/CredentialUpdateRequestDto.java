package in.infosys.backend.dto;
public class CredentialUpdateRequestDto {
 private String title,username,password,
         website,notes,category,credentialType;
 private Boolean favorite;
 public CredentialUpdateRequestDto(){} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getUsername(){return username;} public void setUsername(String v){username=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;} public String getWebsite(){return website;} public void setWebsite(String v){website=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getCredentialType(){return credentialType;} public void setCredentialType(String v){credentialType=v;} public Boolean getFavorite(){return favorite;} public void setFavorite(Boolean v){favorite=v;}
}
