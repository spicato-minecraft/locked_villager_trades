# Release changelog template

Copy this file to `releases/{minecraft_version}-{mod_version}.md` before creating a GitHub Release.

Write for players, not developers. Focus on what changed in the game.

---

## Example

```markdown
- Villagers now keep their locked trades after you break and replace a workstation
- Fixed duplicate trade options sometimes appearing when a villager first gets a profession
```

## Guidelines

- Use plain language ("villagers" not "Villager entity")
- One bullet per player-visible change
- Omit internal refactors, test additions, and CI changes unless players notice them
- Do not copy PR titles or commit messages
