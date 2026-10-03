# YouTube → Instagram Reel Definition of Done

## Input

A valid Flint YouTube source representing an approximately
30-minute video.

## Required pipeline

[ ] Source exists
[ ] Source belongs to current user
[ ] Video metadata available
[ ] Transcript available
[ ] Transcript timestamps valid
[ ] Scene detection complete
[ ] Visual sampling complete
[ ] Semantic segmentation complete
[ ] Reel candidates generated
[ ] Candidate timestamps valid
[ ] Candidate preview works
[ ] User can accept candidate
[ ] User can reject candidate
[ ] User can edit timestamps
[ ] Clip extraction succeeds
[ ] 9:16 conversion succeeds
[ ] Subject framing succeeds or safely falls back
[ ] Captions generated
[ ] Caption timestamps valid
[ ] Hook rendered
[ ] CTA optional
[ ] Final MP4 generated
[ ] Final MP4 is playable
[ ] Audio exists
[ ] Audio/video synchronized
[ ] Output aspect ratio is 9:16
[ ] Output resolution is valid
[ ] Output duration is correct
[ ] Output uploaded to Firebase Storage
[ ] ContentAsset created
[ ] Content Library displays Reel
[ ] User can preview Reel
[ ] User can export Reel

## Failure handling

[ ] Invalid URL
[ ] Missing transcript
[ ] Corrupt media
[ ] FFmpeg failure
[ ] AI failure
[ ] Invalid AI response
[ ] Invalid timestamps
[ ] Rendering failure
[ ] Firebase failure
[ ] Worker restart
[ ] App restart
[ ] Retry

## Regression

[ ] Existing Flint tests pass
[ ] Existing YouTube → Instagram Stage 1 passes
[ ] Existing Content Library passes
[ ] Existing Firebase functionality passes
[ ] Existing AI functionality passes

## Quality

[ ] Unit coverage >= 80% meaningful coverage
[ ] No critical runtime errors
[ ] No unresolved TODOs in production path
[ ] No debug code
[ ] No secrets
[ ] Desktop tested
[ ] Light theme tested
[ ] Dark theme tested