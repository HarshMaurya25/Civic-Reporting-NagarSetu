package com.project.nagarSetu.repository;

import com.project.nagarSetu.entity.IssueStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IssueStageHistoryRepository extends JpaRepository<IssueStageHistory, UUID> {
}
