// Masked PII display component

"use client"

import { useState } from "react"
import { Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"

interface MaskedTextProps {
  value: string
  masked: string
  canReveal?: boolean
  className?: string
}

export function MaskedText({ value, masked, canReveal = true, className }: MaskedTextProps) {
  const [revealed, setRevealed] = useState(false)

  if (!canReveal) {
    return <span className={className}>{masked}</span>
  }

  return (
    <div className="flex items-center gap-2">
      <span className={className}>{revealed ? value : masked}</span>
      <Button variant="ghost" size="sm" onClick={() => setRevealed(!revealed)} className="h-6 w-6 p-0">
        {revealed ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
      </Button>
    </div>
  )
}
