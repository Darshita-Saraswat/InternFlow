"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ApplicationForm } from "@/components/application-form";
import { ApplicationWorkspace } from "@/components/application-workspace";
import { createApplication } from "@/lib/applications-api";

export default function NewApplicationPage() {
  const router = useRouter();

  return (
    <ApplicationWorkspace
      description="Save the details you need to keep moving."
      title="Add an application"
    >
      <section className="application-panel">
        <div className="panel-intro">
          <Link className="text-link" href="/applications">← Back to applications</Link>
          <p>Company, role, and saved date are required. You can add more details whenever you need them.</p>
        </div>
        <ApplicationForm
          onSubmit={async (input) => {
            const created = await createApplication(input);
            router.push(`/applications/${created.id}`);
          }}
          submitLabel="Save application"
        />
      </section>
    </ApplicationWorkspace>
  );
}
