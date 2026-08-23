package com.ainexus.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stepKey;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStepType type;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_step_dependencies", joinColumns = @JoinColumn(name = "step_id"))
    @Column(name = "dependency_key")
    private List<String> dependencies = new ArrayList<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonBackReference
    private Workflow workflow;

    public WorkflowStep() {}

    public WorkflowStep(String stepKey, String name, WorkflowStepType type, String configuration, Integer executionOrder, List<String> dependencies, boolean enabled) {
        this.stepKey = stepKey;
        this.name = name;
        this.type = type;
        this.configuration = configuration;
        this.executionOrder = executionOrder;
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkflowStepType getType() {
        return type;
    }

    public void setType(WorkflowStepType type) {
        this.type = type;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }
}
