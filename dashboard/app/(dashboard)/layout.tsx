import Sidebar from "@/components/sidebar";
import SessionKeepAlive from "@/components/session-keep-alive";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex h-full">
      <SessionKeepAlive />
      <Sidebar />
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  );
}
