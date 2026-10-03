.syntax unified
.arm
.global _start
_start:
  ldr r0, =0x04000000
  ldr r1, =0x0404
  strh r1, [r0]
  ldr r0, =0x06000000
  ldr r2, =38400
  mov r3, #0
initial_pixels:
  strb r3, [r0], #1
  add r3, r3, #1
  subs r2, r2, #1
  bne initial_pixels
  mov r4, #0
frame:
  ldr r0, =0x04000006
wait_blank:
  ldrh r1, [r0]
  cmp r1, #160
  bne wait_blank
wait_frame:
  ldrh r1, [r0]
  cmp r1, #0
  bne wait_frame
  add r4, r4, #1
  ldr r0, =0x04000130
  ldrh r5, [r0]
  mvn r5, r5
  and r5, r5, #255
  ldr r0, =0x05000000
  mov r2, #256
  ldr r6, =0x7fff
  add r3, r4, r5, lsl #8
pixels:
  and r1, r3, r6
  strh r1, [r0], #2
  add r3, r3, #127
  subs r2, r2, #1
  bne pixels
  b frame
.ltorg
