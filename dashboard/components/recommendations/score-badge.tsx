import clsx from "clsx";

export default function ScoreBadge({ score }: { score: number }) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold tabular-nums",
        score >= 80 && "bg-emerald-100 text-emerald-800",
        score >= 60 && score < 80 && "bg-amber-100 text-amber-800",
        score < 60 && "bg-red-100 text-red-800"
      )}
    >
      {score}
    </span>
  );
}
