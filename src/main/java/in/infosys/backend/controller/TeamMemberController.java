package in.infosys.backend.controller;

import in.infosys.backend.dto.TeamMemberRequestDto;
import in.infosys.backend.dto.TeamMemberResponseDto;
import in.infosys.backend.service.TeamMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")  // "/api/team-members"
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService) {
        this.teamMemberService = teamMemberService;
    }

    // =========================================================
    // ADD MEMBER
    // =========================================================
    @PreAuthorize("hasAnyRole('TEAM_MEMBER','ADMINISTRATOR')")
    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponseDto> addMember(
            @PathVariable Long teamId,
            @RequestBody TeamMemberRequestDto request) {

        return ResponseEntity.ok(
                teamMemberService.addMember(
                        teamId,
                        request
                )
        );
    }

    // =========================================================
    // GET MEMBERS
    // =========================================================

    @PreAuthorize("hasAnyRole('TEAM_MEMBER','ADMINISTRATOR')")
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponseDto>> getMembers(
            @PathVariable Long teamId) {

        return ResponseEntity.ok(
                teamMemberService.getMembers(teamId)
        );
    }


}
