# Ludo â€” Free Play Store Checklist

## Cost: $0 to develop and test
- AdMob **test IDs** in debug (switch to your IDs before earning revenue)
- Firebase optional; stub google-services.json included for builds
- English only â€” no translation costs

## ASO (copy into Play Console)
- **Title**: Ludo - Free Puzzle Game
- **Short**: Free offline Ludo with hints, stats, themes, and fair ads.
- **Keywords**: ludo,parcheesi,board game,dice

## Legal (all regions)
- [ ] Host Privacy Policy (free: GitHub Pages)
- [ ] Terms of Service link in app Settings
- [ ] Consent screen for ads/analytics (GDPR/CCPA)
- [ ] Data deletion option in Settings
- [ ] Declare ads in Play Console (AdMob)
- [ ] Target audience: not for children under 13 if ads are personalized
- [ ] Content rating questionnaire

## Monetization (free AdMob account)
1. Create AdMob account (free)
2. Replace test IDs in app/build.gradle.kts release buildConfigFields
3. Use banner + rewarded for hints â€” already wired

## Publishing cost
- Google Play developer fee: **$25 one-time** (only mandatory cash cost)
