# qbit-crm

## Knowledge base

Reviewed dossiers for this repo and the QQQ platform live in the second-brain vault:

- Hub / start here: `R:/Git.Local/KofTwentyTwo/second-brain/knowledge/qqq/qqq-hub.md`
- This repo's dossier: `R:/Git.Local/KofTwentyTwo/second-brain/knowledge/qqq/repos/qbit-crm.md`
  (reviewed commit `76b95c91ee7e`, branch `develop`, 2026-07-04)
- QBit mechanics refresher: `knowledge/qqq/architecture/metadata-model.md` in the same vault

Key facts captured there: parent pom `qbit-build-parent:1.4.0` pins qqq to 0.27.9 (via
`qqq-bom-pom` import); two BREAK-04 compile breaks block a qqq 4.0 upgrade
(`BaseTest.setAuthentication`, `EnrollInSequenceProcess input.getSession()`); Maven
Central's published `0.1.0` predates the AGPL→Apache license switch and the audit fixes.
