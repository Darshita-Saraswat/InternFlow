"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { TaskForm } from "@/components/task-form";
import { createTask } from "@/lib/tasks-api";

export default function NewTaskPage() {
  const router = useRouter();

  return (
    <ApplicationWorkspace activeSection="tasks" description="Capture the next action, its deadline, and what it supports." title="Add a task">
      <section className="application-panel">
        <div className="panel-intro">
          <Link className="text-link" href="/tasks">← Back to tasks</Link>
          <p>A title is required. Link the task to one of your applications when it applies.</p>
        </div>
        <TaskForm
          onSubmit={async (input) => {
            const created = await createTask(input);
            router.push(`/tasks/${created.id}`);
          }}
          submitLabel="Save task"
        />
      </section>
    </ApplicationWorkspace>
  );
}
