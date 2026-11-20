import { expect, describe, it, vi } from 'vitest';
import { createApi } from './index.js';

// The host context may expose `$checkPersistence`. The runtime reports
// 'runtime-ready' once and then every permission it has granted, so the host
// can refuse a write (by throwing) while saving cannot be trusted.
describe('Plugin api: host persistence hook', () => {
  function pluginManager(context: Record<string, unknown>) {
    return {
      manifest: {
        pluginId: 'test',
        name: 'test',
        code: '',
        host: 'http://fake.fake/',
        permissions: ['content:read', 'content:write'],
      },
      context,
    } as any;
  }

  it('reports runtime-ready and each granted permission to the host', () => {
    const hook = vi.fn();
    const createRectangle = vi.fn().mockReturnValue('rect');
    const api = createApi(
      pluginManager({ $checkPersistence: hook, createRectangle }),
    );

    expect(hook.mock.calls).toEqual([['runtime-ready']]);
    expect(api.penpot.createRectangle()).toBe('rect');
    expect(hook.mock.calls).toEqual([['runtime-ready'], ['content:write']]);
  });

  it('a refusal from the host stops the write before it reaches the context', () => {
    const createRectangle = vi.fn();
    const hook = vi.fn((permission: string) => {
      if (permission === 'content:write') {
        throw new Error('DS_SAVE_BLOCKED');
      }
    });
    const api = createApi(
      pluginManager({ $checkPersistence: hook, createRectangle }),
    );

    expect(() => api.penpot.createRectangle()).toThrow('DS_SAVE_BLOCKED');
    expect(createRectangle).not.toHaveBeenCalled();
  });

  it('a permission that is not granted is refused before the host is asked', () => {
    const hook = vi.fn();
    const api = createApi(pluginManager({ $checkPersistence: hook }));

    expect(() => api.penpot.library.local).toThrow(
      'Permission library:read is not granted',
    );
    expect(hook.mock.calls).toEqual([['runtime-ready']]);
  });

  it('works without the hook', () => {
    const createRectangle = vi.fn().mockReturnValue('rect');
    const api = createApi(pluginManager({ createRectangle }));

    expect(api.penpot.createRectangle()).toBe('rect');
  });
});
