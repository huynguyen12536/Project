package com.learnhub.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "file_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID snapshotId;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer lineCount;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String content;

    public FileSnapshot(String name, Integer lineCount, String content) {
        this.name = name;
        this.lineCount = lineCount;
        this.content = content;
        this.path = name;
    }
}
